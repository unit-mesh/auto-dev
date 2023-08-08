from urllib.request import Request

import requests
import sseclient
from fastapi import FastAPI, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from starlette import status
from starlette.responses import JSONResponse

app = FastAPI()


async def fetch_chat_completion(prompt: str):
    url = "https://api.aios.chat/v1/chat/completions"
    headers = {
        'Accept': 'text/event-stream',
        "Authorization": "",
    }
    data = {
        "model": "gpt-3.5-turbo",
        "messages": [
            {"role": "user", "content": prompt}
        ],
        "temperature": 0,
        "max_tokens": 4000,
        "stream": True,  # Set stream to True
    }

    response = requests.post(url, headers=headers, json=data, stream=True)
    client = sseclient.SSEClient(response)

    for event in client.events():
        try:
            yield 'data: ' + event.data + '\n\n'
        except Exception as e:
            print("OpenAI Response (Streaming) Error: " + str(e))

class ChatInput(BaseModel):
    instruction: str
    input: str


@app.post("/api/chat", response_class=Response)
async def chat(input: ChatInput):
    print(input)
    return StreamingResponse(fetch_chat_completion(input.instruction), media_type="text/event-stream")
