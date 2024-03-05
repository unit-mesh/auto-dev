from typing import List

import uvicorn
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from pydantic import BaseModel
from starlette import status
from starlette.requests import Request
from starlette.responses import JSONResponse

app = FastAPI()


class Message(BaseModel):
    role: str
    message: str


class Messages(BaseModel):
    messages: List[Message]

@app.post("/api/agent/market")
def mock_market(messages: Messages):
    return "I am a mock market agent for intention: " + messages.messages[0].message


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    print(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


if __name__ == "__main__":
    uvicorn.run(app, port=8765)
