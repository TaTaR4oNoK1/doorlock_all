from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import psycopg2
from psycopg2.extras import RealDictCursor
from datetime import datetime, timedelta, date
import random
import re

from send_verification_email import send_verification_email
from config import get_db_connection
from schemas import AuthRequest, ValidateRequest, SendCodeRequest, VerifyCodeRequest, ChangePasswordRequest, AddressRequest

app = FastAPI()

# Аутентификация пользователя
@app.post("/auth")
def authenticate(request: AuthRequest):
    conn = get_db_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(
                "SELECT id FROM users WHERE login = %s AND password = %s",
                (request.login, request.password)
            )
            user = cur.fetchone()

        if not user:
            return {"session_code": None}

        user_id = user["id"]
        rand_code = random.randint(100000, 999999) + int(datetime.now().timestamp())
        session_code = hex(rand_code)[2:]
        end_time = datetime.now() + timedelta(days=90)

        with conn.cursor() as cur:
            cur.execute(
                """INSERT INTO authorizations
                (session_code, end_time, user_id, ip_address)
                VALUES (%s, %s, %s, %s)""",
                (session_code, end_time, user_id, request.ip_address)
            )
        conn.commit()
        return {"session_code": session_code, "user_id": user_id}
    except Exception as e:
        conn.rollback() 
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# Проверка валидации сессии
@app.post("/validate")
def validate_session(request: ValidateRequest):
    conn = get_db_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(
                """SELECT user_id, end_time FROM authorizations
                WHERE session_code = %s""",
                (request.session_code,)
            )
            session = cur.fetchone()

        if not session:
            raise HTTPException(status_code=404, detail="Сессия не найдена")

        end_time = session["end_time"]
        if end_time < datetime.now():
            raise HTTPException(status_code=401, detail="Сессия истекла")

        return {"user_id": session["user_id"]}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

#Работа с адресами
@app.post("/adress-help")
def address_help(request: AddressRequest): 
    user_id = int(request.user_id)
    help_type = request.type
    
    if help_type not in ("add_address", "change_address", "delete_address", "get_addresses"):
        raise HTTPException(status_code=400, detail="Неверный тип операции (type)")
        
    conn = get_db_connection()
    try:
        # Добавление адреса
        if help_type == "add_address":
            with conn.cursor() as cur:
                cur.execute(
                    "INSERT INTO addresses (title, user_id) VALUES (%s, %s)",
                    (request.title, user_id)
                )
            conn.commit()
            return {"success": True, "message": "Адрес добавлен"}
            
        # Получение списка адресов (id + title)
        elif help_type == "get_addresses":       
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    "SELECT id, title FROM addresses WHERE user_id = %s", 
                    (user_id,)
                )
                rows = cur.fetchall()
            return rows
            
        # Изменение адреса
        elif help_type == "change_address":       
            with conn.cursor() as cur:
                cur.execute(
                    "UPDATE addresses SET title = %s WHERE user_id = %s AND id = %s",
                    (request.title, user_id, request.address_id)
                )
            conn.commit()
            return {"success": True, "message": "Адрес изменен"}
            
        # Удаление адреса
        elif help_type == "delete_address":       
            with conn.cursor() as cur:
                cur.execute(
                    "DELETE FROM addresses WHERE user_id = %s AND id = %s",
                    (user_id, request.address_id)
                )
            conn.commit()
            return {"success": True, "message": "Адрес удален"}

    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=f"Ошибка БД: {str(e)}")
    finally:
        close_connection(conn)   

# Отправка кода (с ограничением на 5 ЗАПРОСОВ кода и блокировкой на 1 час)
@app.post("/send-code")
def send_code_endpoint(request: SendCodeRequest):
    email_clean = normalize_email(request.email)
    validate_email_format(email_clean)
    
    if request.type not in ("recovery", "registration"):
        raise HTTPException(status_code=400, detail="Неверный тип операции (type)")

    conn = get_db_connection()
    try:
        # 1. Проверяем блокировку по времени и количество попыток ЗАПРОСА кода
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(
                "SELECT code_blocked_until, send_code_attempts FROM users WHERE LOWER(TRIM(mail)) = %s", 
                (email_clean,)
            )
            user_data = cur.fetchone()
            
            if user_data:
                # Если стоит временная блокировка
                if user_data["code_blocked_until"] and user_data["code_blocked_until"] > datetime.now():
                    remaining_time = user_data["code_blocked_until"] - datetime.now()
                    minutes_left = int(remaining_time.total_seconds() // 60)
                    raise HTTPException(
                        status_code=429, 
                        detail=f"Превышено количество попыток. Отправка заблокирована. Попробуйте через {minutes_left} мин."
                    )
                
                # Если временной блокировки нет, но попыток запроса уже >= 5
                if (user_data["send_code_attempts"] or 0) >= 5:
                    block_until = datetime.now() + timedelta(hours=1)
                    cur.execute(
                        "UPDATE users SET code_blocked_until = %s, send_code_attempts = 0 WHERE LOWER(TRIM(mail)) = %s",
                        (block_until, email_clean)
                    )
                    conn.commit()
                    raise HTTPException(
                        status_code=429,
                        detail="Вы слишком часто запрашивали код. Функция отправки заблокирована на 1 час."
                    )

        # 2. Логика обработки типов запроса
        if request.type == "recovery":
            check_field_existence(conn, field_name="mail", value=email_clean, display_name="Email", raise_if_exists=False)
            
            # Увеличиваем счетчик запросов кода восстановления
            with conn.cursor() as cur:
                cur.execute(
                    "UPDATE users SET send_code_attempts = COALESCE(send_code_attempts, 0) + 1 WHERE LOWER(TRIM(mail)) = %s",
                    (email_clean,)
                )
            send_code(conn, email_clean, 'recovery', user_information={})
            message = "Код восстановления отправлен"
            
        elif request.type == "registration":
            if not all([request.login, request.name, request.surname, request.birth_date, request.sex, request.phone]):
                raise HTTPException(status_code=400, detail="Для регистрации необходимо заполнить все поля профиля")                
            
            login_clean = request.login.strip()
            check_field_existence(conn, field_name="mail", value=email_clean, display_name="Email", raise_if_exists=True)        
            check_field_existence(conn, field_name="login", value=login_clean, display_name="Login", raise_if_exists=True)
            
            user_information = {
                'name': request.name, 'surname': request.surname, 'birth_date': request.birth_date, 
                'login': login_clean, 'sex': request.sex, 'phone': request.phone
            }
            # При регистрации запись создается впервые, поэтому send_code_attempts ставим в 1 внутри функции записи
            send_code(conn, email_clean, request.type, user_information)
            message = "Код регистрации отправлен"

        conn.commit()
        print(f"[INFO] {message} на {email_clean}")
        return {"success": True, "message": message}
        
    except HTTPException:
        conn.rollback()
        raise
    except Exception as e:
        conn.rollback()
        handle_exception(e, email_clean)
    finally:
        close_connection(conn)

# Проверка кода ввода (не более 5 ошибок)
@app.post("/check-code")
def verify_code(request: VerifyCodeRequest):
    conn = get_db_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(
                """
                SELECT recovery_code, recovery_code_expire, 
                       verification_code, verification_code_expire, 
                       code_attempts, code_blocked_until 
                FROM users WHERE mail = %s
                """,
                (request.email,)
            )
            user = cur.fetchone()

        if not user:
            raise HTTPException(status_code=404, detail="Пользователь не найден")

        if user["code_blocked_until"] and user["code_blocked_until"] > datetime.now():
            raise HTTPException(status_code=429, detail="Проверка заблокирована. Попробуйте позже.")

        if request.type_code == "recovery":
            stored_code = user["recovery_code"]
            expire_time = user["recovery_code_expire"]
            error_message = "Неверный код восстановления"
        elif request.type_code == "registration":
            stored_code = user["verification_code"]
            expire_time = user["verification_code_expire"]
            error_message = "Неверный код подтверждения регистрации"
        else:
            raise HTTPException(status_code=400, detail="Неподдерживаемый тип кода")

        if not expire_time or datetime.now() > expire_time:
            raise HTTPException(status_code=400, detail="Срок действия кода изпек. Запросите код заново.")

        # Если код введен неверно
        if stored_code != request.code:
            new_attempts = (user["code_attempts"] or 0) + 1
            
            with conn.cursor() as cur:
                if new_attempts >= 5:
                    block_until = datetime.now() + timedelta(hours=1)
                    cur.execute(
                        "UPDATE users SET code_attempts = %s, code_blocked_until = %s WHERE mail = %s",
                        (new_attempts, block_until, request.email)
                    )
                    conn.commit()
                    raise HTTPException(status_code=429, detail="Превышено 5 попыток ввода. Доступ заблокирован на 1 час.")
                else:
                    cur.execute("UPDATE users SET code_attempts = %s WHERE mail = %s", (new_attempts, request.email))
                    conn.commit()
                    raise HTTPException(status_code=400, detail=f"{error_message}. Осталось попыток: {5 - new_attempts}")

        # ЕСЛИ КОД ВЕРНЫЙ — сбрасываем и попытки ввода, и попытки ЗАПРОСА кода!
        with conn.cursor() as cur:
            cur.execute(
                """
                UPDATE users 
                SET code_attempts = 0, 
                    send_code_attempts = 0, 
                    code_blocked_until = NULL 
                WHERE mail = %s
                """, 
                (request.email,)
            )
        conn.commit()

        return {"success": True, "message": "Код подтверждён"}

    except HTTPException:
        conn.rollback()
        raise  
    except Exception as e:
        conn.rollback()
        print(f"[ERROR] Ошибка при проверке кода для {request.email}: {str(e)}")
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера.")
    finally:
        close_connection(conn)

# Сохранение пароля
@app.post("/change-password")
def change_password(request: ChangePasswordRequest):
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE users SET password = %s WHERE mail = %s",
                (request.password, request.email)
            )
            if cur.rowcount == 0:
                raise HTTPException(status_code=404, detail="Пользователь не найден")
        conn.commit()
        return {"success": True, "message": "Пароль успешно изменён"}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        close_connection(conn)


def handle_exception(exception: Exception, email: str):
    print(f"[ERROR] Ошибка при отправке кода для {email}: {str(exception)}")
    raise HTTPException(
        status_code=500,
        detail="Внутренняя ошибка сервера. Попробуйте позже"
    )

def close_connection(conn):
    if conn:
        try:
            conn.close()
        except Exception as close_error:
            print(f"[CONNECTION CLOSE ERROR] Ошибка при закрытии соединения: {str(close_error)}")


def code_generator():    
    recovery_code = f"{random.randint(100000, 999999):06d}"
    return recovery_code


def recovery_code_writing(conn, email: str, code: str) -> None:
    expire_time = datetime.now() + timedelta(minutes=2)
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE users 
            SET recovery_code = %s, 
                recovery_code_expire = %s,
                code_attempts = 0 
            WHERE LOWER(TRIM(mail)) = %s
            """,
            (code, expire_time, email)
        )

def verify_code_writing(conn, email: str, code: str, user_information: dict) -> None:
    time_password = hex(random.randint(100000000, 999999999))[2:] 
    reg_day = datetime.now().date()
    expire_time = datetime.now() + timedelta(minutes=2)

    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO users (
                name, surname, birth_date, login, mail, sex, phone, password, 
                registration_date, verification_code, verification_code_expire, code_attempts, send_code_attempts
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 0, 1)
            """,
            (
                user_information.get('name'),
                user_information.get('surname'),
                user_information.get('birth_date'),
                user_information.get('login'),
                email,
                user_information.get('sex'),
                user_information.get('phone'),
                time_password,
                reg_day,
                code,
                expire_time
            )
        )

def check_field_existence(conn, field_name: str, value: str, display_name: str, raise_if_exists: bool = False) -> bool:
    if field_name not in ("mail", "login"):
        raise ValueError("Недопустимое имя поля для проверки")

    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        cur.execute(
            f"SELECT id FROM users WHERE LOWER(TRIM({field_name})) = %s",
            (value.strip().lower(),)
        )
        user = cur.fetchone()
        exists = user is not None

    if raise_if_exists and exists:
        raise HTTPException(
            status_code=400,
            detail=f"{display_name} уже зарегистрирован в системе"
        )
    elif not raise_if_exists and not exists:
        raise HTTPException(
            status_code=404,
            detail=f"{display_name} не найден в системе"
        )
    return exists

def send_code(conn, email: str, code_type: str, user_information: dict) -> str:
    code = code_generator()

    if code_type == 'recovery':
        recovery_code_writing(conn, email, code)
    elif code_type == 'registration':
        verify_code_writing(conn, email, code, user_information)

    send_verification_email(email, code)
    return code

def normalize_email(email: str) -> str:
    return email.strip().lower()

def validate_email_format(email: str) -> None:
    if not email or not re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$", email):
        raise HTTPException(
            status_code=400,
            detail="Некорректный формат email"
        )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=6000)