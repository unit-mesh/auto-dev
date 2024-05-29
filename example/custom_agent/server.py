from typing import List

import uvicorn
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from pydantic import BaseModel
from starlette import status
from starlette.requests import Request
from starlette.responses import JSONResponse, HTMLResponse, PlainTextResponse

app = FastAPI()


class Message(BaseModel):
    role: str
    content: str


class Messages(BaseModel):
    messages: List[Message]


@app.post("/api/agent/auto-test", response_class=PlainTextResponse)
def mock_market(messages: Messages):
    return """
用户故事:

| 编号 | AC简述 | GIVEN | WHEN | THEN | 建议优先级 |
| --- | --- | --- | --- | --- | --- |
| 1 | 财务经理设定新的支付限额 | 财务经理已登录到银行现金管理系统并选择了一个尚未设定支付限额的成员单位账户 | 财务经理输入了新的支付限额并提交 | 系统接受新的支付限额并显示成功的确认消息，同时更新账户的支付限额 | 高 |
| 2 | 财务经理查看支付限额建议 | 财务经理已登录到银行现金管理系统并选择了一个尚未设定支付限额的成员单位账户 | 财务经理尝试设定支付限额 | 系统自动计算并显示建议的支付限额 | 高 |
| 3 | 财务经理尝试支付超过设定限额的金额 | 财务经理已登录到银行现金管理系统并选择了一个已设定支付限额的成员单位账户，尝试支付超过设定限额的金额 | 财务经理提交支付请求 | 系统拒绝支付请求并显示一个警告消息，指出支付金额超过了设定的支付限额 | 中 |

**Sad Path：**

| 编号 | AC简述 | GIVEN | WHEN | THEN | 建议优先级 |
| --- | --- | --- | --- | --- | --- |
| 1 | 财务经理尝试设定不存在的成员单位账户的支付限额 | 财务经理已登录到银行现金管理系统 | 财务经理选择一个不存在的成员单位账户并尝试设定支付限额 | 系统拒绝设定支付限额并显示一个错误消息，指出所选的成员单位账户不存在 | 高 |
| 2 | 财务经理尝试设定一个无效的支付限额 | 财务经理已登录到银行现金管理系统并选择了一个成员单位账户 | 财务经理输入了一个无效的支付限额（例如非数字、负数等）并尝试提交 | 系统拒绝设定支付限额并显示一个错误消息，指出支付限额必须是有效的数值 | 高 |
| 3 | 财务经理尝试设定超过账户余额的支付限额 | 财务经理已登录到银行现金管理系统并选择了一个成员单位账户，账户余额为X元 | 财务经理输入了一个大于X元的支付限额并尝试提交 | 系统拒绝设定支付限额并显示一个错误消息，指出支付限额不能超过账户的余额 | 中 |

**Exceptional Path：**

| 编号 | AC简述 | GIVEN | WHEN | THEN | 建议优先级 |
| --- | --- | --- | --- | --- | --- |
| 1 | 网络异常 | 财务经理已登录到银行现金管理系统并选择了一个成员单位账户 | 在设定支付限额时发生网络异常 | 系统显示一个错误消息，指出网络异常，让用户稍后再试 | 高 |
| 2 | 后端服务异常 | 财务经理已登录到银行现金管理系统并选择了一个成员单位账户 | 在设定支付限额时后端服务异常 | 系统显示一个错误消息，指出服务暂时不可用，让用户稍后再试 | 高 |
| 3 | 数据库异常 | 财务经理已登录到银行现金管理系统并选择了一个成员单位账户 | 在设定支付限额时数据库异常 | 系统显示一个错误消息，指出数据异常，让用户稍后再试 | 高 |
"""


@app.post("/api/agent/api-market", response_class=PlainTextResponse)
def mock_market(messages: Messages):
    return """
Here are the APIs you can use:

```markdown
### Account Endpoints

#### GET /accounts
- **Input**: 
  {
    "headers": {
      "Authorization": "Bearer {access_token}"
    }
  }
- **Output**: 
  [
    {
      "id": "12345",
      "name": "Checking Account",
      "balance": 1500.75,
      "currency": "USD",
      "type": "checking"
    },
    {
      "id": "67890",
      "name": "Savings Account",
      "balance": 5000.00,
      "currency": "USD",
      "type": "savings"
    }
  ]

#### GET /accounts/{id}
- **Input**: 
  {
    "headers": {
      "Authorization": "Bearer {access_token}"
    }
  }
- **Output**: 
  {
    "id": "12345",
    "name": "Checking Account",
    "balance": 1500.75,
    "currency": "USD",
    "type": "checking"
  }

#### POST /accounts
- **Input**: 
  {
    "headers": {
      "Authorization": "Bearer {access_token}"
    },
    "body": {
      "name": "New Account",
      "type": "checking",
      "currency": "USD",
      "initial_balance": 1000.00
    }
  }
- **Output**: 
  {
    "id": "11223",
    "name": "New Account",
    "balance": 1000.00,
    "currency": "USD",
    "type": "checking"
  }

#### PUT /accounts/{id}
- **Input**: 
  {
    "headers": {
      "Authorization": "Bearer {access_token}"
    },
    "body": {
      "name": "Updated Account Name",
      "balance": 2000.00,
      "currency": "USD"
    }
  }
- **Output**: 
  {
    "id": "12345",
    "name": "Updated Account Name",
    "balance": 2000.00,
    "currency": "USD",
    "type": "checking"
  }
#### DELETE /accounts/{id}
- **Input**: 
  {
    "headers": {
      "Authorization": "Bearer {access_token}"
    }
  }
  ```
- **Output**: 
  {
    "status": "204 No Content"
  }
```"""


@app.post("/api/agent/component-list", response_class=PlainTextResponse)
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


@app.get("/api/agent/ux", response_class=HTMLResponse)
def mock_frontend():
    return mock_html()


@app.post("/api/agent/ux", response_class=HTMLResponse)
def mock_frontend(messages: Messages):
    return mock_html()


def mock_html():
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


@app.post("/api/agent/devins-sample", response_class=PlainTextResponse)
def mock_devins(messages: Messages):
    return """
Here are the patches for your

```devin
/patch

\\`\\`\\`patch
Index: src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java
IDEA additional info:
Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
<+>UTF-8
===================================================================
diff --git a/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java b/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java
--- a/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java	(revision b5985862c79fe42043697bc5d5f38b470020be3d)
+++ b/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java	(date 1709616724534)
@@ -4,7 +4,19 @@
 
 @RestController
 public class BlogCategoryController {
-    // devti://story/github/1
+    public void addCategory(String categoryName) {
+        // ... add category logic here
+    }
+
+    public void deleteCategory(long categoryId) {
+        // ... delete category logic here
+    }
 
-    // Close a bank account
+    public void updateCategory(long categoryId, String newCategoryName) {
+        // ... update category logic here
+    }
+
+    public void listCategories() {
+        // ... list all categories logic here
+    }
 }
\\`\\`\\`
```"""


@app.post("/api/agent/devins-write", response_class=PlainTextResponse)
def mock_devins(messages: Messages):
    return """
```devin
/write:src/main/java/com/booking/BookingController.java#L1-L2

\\`\\`\\`java
/**
 * ===========================================================================
*/
\\`\\`\\`
```"""


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    print(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


if __name__ == "__main__":
    uvicorn.run(app, port=8765)
