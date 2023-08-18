from urllib.request import Request

import requests
import sseclient
from fastapi import FastAPI, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List
from starlette import status
from starlette.responses import JSONResponse

app = FastAPI()


class Message(BaseModel):
    role: str
    message: str


class ChatInput(BaseModel):
    messages: List[Message]


async def fetch_chat_completion(messages: List[Message]):
    url = "https://api.aios.chat/v1/chat/completions"
    headers = {
        'Accept': 'text/event-stream',
        "Authorization": "",
    }
    data = {
        "model": "gpt-3.5-turbo",
        "messages": [
            {"role": "user", "content": messages[-1].message},
        ],
        "temperature": 0,
        "max_tokens": 4000,
        "stream": True,  # Set stream to True
    }

    response = requests.post(url, headers=headers, json=data, stream=True)
    client = sseclient.SSEClient(response)

    for event in client.events():
        try:
            print(event.data)
            yield 'data: ' + event.data + '\n\n'
        except Exception as e:
            print("OpenAI Response (Streaming) Error: " + str(e))


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    print(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/api/chat", response_class=Response)
async def chat(msg: ChatInput):
    return StreamingResponse(fetch_chat_completion(msg.messages), media_type="text/event-stream")
