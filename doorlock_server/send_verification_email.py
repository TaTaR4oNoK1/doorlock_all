import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

def send_verification_email(to_email: str, code: str):
    SMTP_SERVER = "smtp.mail.ru"
    SMTP_PORT = 587  # предпочтительный порт с TLS
    SMTP_USERNAME = "doorlock.official@mail.ru"
    SMTP_PASSWORD = "o7SCIDfBRaJjjg1kHxz8"  # замените на пароль приложения!

    subject = "Ваш код верификации"
    body = f"""
Здравствуйте!

Ваш код доступа: {code}

Если вы не запрашивали его, проигнорируйте это письмо.

С уважением,
Команда сервиса
"""

    msg = MIMEMultipart()
    msg["From"] = SMTP_USERNAME
    msg["To"] = to_email
    msg["Subject"] = subject
    msg.attach(MIMEText(body, "plain", "utf-8"))

    try:
        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT, timeout=30)
        server.starttls()  # включаем TLS для порта 587
        server.set_debuglevel(1)  # детальное логирование SMTP

        server.login(SMTP_USERNAME, SMTP_PASSWORD)

        text = msg.as_string()
        server.sendmail(SMTP_USERNAME, to_email, text)
        server.quit()
        return True

    except smtplib.SMTPAuthenticationError as e:
        raise Exception("Ошибка авторизации SMTP. Проверьте логин/пароль.")
    except Exception as e:
        raise Exception(f"Ошибка отправки email: {str(e)}")
