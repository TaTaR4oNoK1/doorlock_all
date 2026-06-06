import psycopg2
from fastapi import HTTPException

DB_CONFIG = {
    "host": "45.11.26.157",
    "port": 5433,
    "database": "mydb",
    "user": "myuser",
    "password": "mysecretpassword"
}

def get_db_connection():
    try:
        return psycopg2.connect(
            host=DB_CONFIG["host"],
            port=DB_CONFIG["port"],
            database=DB_CONFIG["database"],
            user=DB_CONFIG["user"],
            password=DB_CONFIG["password"]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ошибка подключения к БД: {e}")