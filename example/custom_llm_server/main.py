import json
from typing import List
from urllib.request import Request

import requests
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from pydantic import BaseModel
from starlette import status
from starlette.responses import JSONResponse

app = FastAPI()


class Message(BaseModel):
    role: str
    content: str


class CustomInput(BaseModel):
    text: str
    history: List[str]


# "application/json; charset=utf-8".toMediaTypeOrNull(),
#             """
#                 {
#                     "instruction": "$instruction",
#                     "input": "$input",
#                 }
#             """.trimIndent()
class MessageInput(BaseModel):
    instruction: str
    input: str


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    print(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/chat")
async def chat(data: MessageInput):
    print(data)

    input = {
        "text": data.instruction,
        "history": []
    }

    response = requests.post("http://10.207.8.18:11000/components/easy_chat/api/predict/", json={
        "data": [json.dumps(input), 2000, 0.1, 0.1, "chatglm_4bit_seldon"]
    }).json()

    print(response["data"][0])
    return response["data"][0]
