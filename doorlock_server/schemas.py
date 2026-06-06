from pydantic import BaseModel
from datetime import date

class AuthRequest(BaseModel):
    login: str
    password: str
    ip_address: str = "000.000.000.000:0000"

class ValidateRequest(BaseModel):
    session_code: str

class SendCodeRequest(BaseModel):
    email: str
    type: str 
    name: str | None = None
    surname: str | None = None
    birth_date: date | None = None
    login: str | None = None
    sex: str | None = None
    phone: str | None = None

class VerifyCodeRequest(BaseModel):
    email: str
    code: str
    type_code: str

class ChangePasswordRequest(BaseModel):
    email: str
    password: str

class AddressRequest(BaseModel):
    user_id: str
    type: str     
    title: str | None = None         
    address_id: str | None = None