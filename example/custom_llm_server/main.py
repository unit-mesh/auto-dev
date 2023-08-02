import json
import requests
from typing import Union, List
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()


class Message(BaseModel):
    role: str
    content: str


class CustomInput(BaseModel):
    text: str
    history: List[str]


@app.post("/chat")
async def chat(messages: List[Message]):
    input = {
        "text": messages[-1].content,
        "history": []
    }

    response = requests.post("http://10.207.8.18:11000/components/easy_chat/api/predict/", json={
        "data": [json.dumps(input), 2000, 0.1, 0.1, "chatglm_4bit_seldon"]
    }).json()

    return response
