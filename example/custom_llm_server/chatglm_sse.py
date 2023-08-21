import os
from typing import List

import uvicorn
from fastapi import FastAPI, APIRouter
from gluon_meson_components.models.chat_model import ChatModel
from pydantic import BaseModel
from util import convert_history
from sse_starlette.sse import EventSourceResponse

PLAIN_MODEL_TYPE = os.getenv('DEFAULT_PLAIN_MODEL_TYPE', 'chatglm2-6b')
STREAM_MODEL_TYPE = os.getenv('DEFAULT_STREAM_MODEL_TYPE', f'{PLAIN_MODEL_TYPE}_streaming')
FLASH_ENV = os.getenv('FLASK_ENV')
APP_PORT = 8000

chat_model = ChatModel()

app = FastAPI()
router = APIRouter()


class MessageInChat(BaseModel):
    role: str
    message: str


class MessageInResponseChat(BaseModel):
    role: str
    content: str


class ChatCommand(BaseModel):
    messages: List[MessageInChat]
    model: str = STREAM_MODEL_TYPE


class ChatResponse(BaseModel):
    choices: List[MessageInResponseChat]
    model: str


def parse_request(chat_command: ChatCommand):
    model = chat_command.model
    messages = chat_command.messages
    text = messages.pop().message
    history = convert_history([i.__dict__ for i in messages])
    return model, text, history


@router.post('/messages/stream')
def stream(chat_command: ChatCommand):
    model, text, history = parse_request(chat_command)
    history = list(map(tuple, history))

    def handle():
        previous_response_len = 0
        for response in chat_model.chat_single_streaming(text=text, model_type=model, history=history):
            response_data = response.response[previous_response_len:]
            previous_response_len = len(response.response)
            yield ChatResponse(choices=[MessageInResponseChat(role='assistant', content=response_data)],
                               model=model).json()
        yield {'data': '[DONE]'}

    return EventSourceResponse(handle())


app.include_router(router)

if __name__ == '__main__':
    if FLASH_ENV == 'production':
        print(f"Starting server in production mode at port {APP_PORT}")
        uvicorn.run(app, host="0.0.0.0", port=APP_PORT)
    else:
        uvicorn.run(app, host="0.0.0.0", port=APP_PORT, log_level='debug')
