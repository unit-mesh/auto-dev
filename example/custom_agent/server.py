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


@app.post("/api/agent/api-market")
def mock_market(messages: Messages):
    return """```markdown
GET /wp/v2/posts
GET /wp/v2/posts/{id}
POST /wp/v2/posts
PUT /wp/v2/posts/{id}
DELETE /wp/v2/posts/{id}

GET /wp/v2/pages
GET /wp/v2/pages/{id}
POST /wp/v2/pages
PUT /wp/v2/pages/{id}
DELETE /wp/v2/pages/{id}
```"""


@app.post("/api/agent/component-list")
def mock_frontend(messages: Messages):
    return """```markdown
Button:可快速创建不同样式的按钮。
Checkbox:提供多选框组件，通常用于某选项的打开或关闭。
CheckboxGroup:多选框群组，用于控制多选框全选或者不全选状态。
CustomDialog:自定义弹窗（CustomDialog）可用于广告、中奖、警告、软件更新等与用户交互响应操作。
Image:Image为图片组件，常用于在应用中显示图片。Image支持加载string、PixelMap和Resource类型的数据源，支持png、jpg、bmp、svg和gif类型的图片格式。
Menu:以垂直列表形式显示的菜单。
Popup:Popup属性可绑定在组件上显示气泡弹窗提示，设置弹窗内容、交互逻辑和显示状态。
Progress:Progress 是进度条显示组件，显示内容通常为某次目标操作的当前进度。
Text:显示一段文本的组件。。
TextArea:多行文本输入框。
TextInput:单行文本输入框组件。
Radio:提供相应的用户交互选择项。
Toggle:Toggle为开关组件，常用于在应用中进行开关操作。
```"""


@app.post("/api/agent/ux")
def mock_frontend(messages: Messages):
    return """
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Document</title>
</head>
<body>
    <h1>Hello,World</h1>
    <p>你好，世界</p>
</body>
</html>
"""


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    print(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


if __name__ == "__main__":
    uvicorn.run(app, port=8765)
