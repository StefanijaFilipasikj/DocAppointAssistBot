from typing import List
from langchain.schema import HumanMessage, AIMessage, SystemMessage, BaseMessage
from models import Message

def convert_to_langchain_messages(messages: List[Message]) -> List[BaseMessage]:
    role_map = {
        "USER": HumanMessage,
        "CHATBOT": AIMessage,
        "SYSTEM": SystemMessage
    }
    return [role_map[msg.role](content=msg.content) for msg in messages]