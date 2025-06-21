from pydantic import BaseModel
from typing import List

class Message(BaseModel):
    content: str
    role: str

class MessagesList(BaseModel):
    messages: List[Message]
    role: str
    patient_id: str

class PatientReport(BaseModel):
    text: str
    patient_id: str
    date: str