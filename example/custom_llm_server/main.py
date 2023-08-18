from typing import List
from urllib.request import Request

import requests
import sseclient
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from pydantic import BaseModel
from starlette import status
from starlette.responses import JSONResponse

app = FastAPI()


class Message(BaseModel):
    role: str
    content: str

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    print(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/chat")
async def chat(data: List[Message]):
    headers = {
        'Accept': 'text/event-stream',
        "Authorization": "",
    }

    response = requests.post("http://sz.private.gluon-meson.tech:8000/messages/stream", headers=headers, json=data, stream=True)
    client = sseclient.SSEClient(response)

    for event in client.events():
        try:
            yield 'data: ' + event.data + '\n\n'
        except Exception as e:
            print("OpenAI Response (Streaming) Error: " + str(e))

