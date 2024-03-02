---
layout: default
title: Custom LLm Server
parent: Customize Features
nav_order: 14
permalink: /custom/llm-server
---

# Custom LLM Server

## Custom response format

We used [JsonPathKt](https://github.com/codeniko/JsonPathKt) to parse response,
currently we only extract the first choice and only the response message.
If your response is this format: 

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1677652288,
  "model": "gpt-3.5-turbo",
  "choices": [{
    "index": 0,
    "delta": {
      "content": "Hello"
    },
    "finish_reason": "stop"
  }]
}
```
You need to set the `response format` to:

```text
$.choices[0].message.delta.content
```

## Custom request format

Only support number of request parameters like OpenAI does.
Only support http request that doesn't need encryption keys(like websocket)

### Custom Request (header/body/message-keys)

**BE CAREFUL: In this project, messageKey is not compatible with openAI: messageKeys: `{ { "content": "content" } }`is REQUIRED** *maybe we will fix this in the future.*

If your llm server has a custom request format, you can:

- Add top level field to the request body via `customFields`
- Add custom headers to the request via `customHeaders`
- Customize the messages key via `messageKeys`

For example:

```json
{ "customFields": {"user": "12345", "model":"model-name", "stream": true},  "messageKeys": { "content": "content" }}
```

Or with custom headers:

```json
{
  "customHeaders": { "CustomHeader": "my-value" },
  "customFields": {"user": "12345", "model": "gpt-4"},
  "messageKeys": {"role": "role", "content": "message"}
}
```

Request header will be( origin key is omitted here):

```http-request
POST https://your.server.com/path

CustomHeader: my-value
...(other headers)
```

And the request body will be:

```json
{
  "user": "12345",
  "model": "gpt-4",
  "messages": [
    {
      "role": "user",
      "message": "..."
    }
  ]
}
```

### Moonshot AI examples

Official Moonshot AI doc: https://platform.moonshot.cn/docs/api-reference

Official API example:

```
from openai import OpenAI

client = OpenAI(
    api_key="MOONSHOT_API_KEY",
    base_url="https://api.moonshot.cn/v1",
)

completion = client.chat.completions.create(
  model="moonshot-v1-8k",
  messages=[ 
    {"role": "system", "content": "你是 Kimi，由 Moonshot AI 提供的人工智能助手，你更擅长中文和英文的对话。你会为用户提供安全，有帮助，准确的回答。同时，你会拒绝一些涉及恐怖主义，种族歧视，黄色暴力等问题的回答。Moonshot AI 为专有名词，不可翻译成其他语言。"},
    {"role": "user", "content": "你好，我叫李雷，1+1等于多少？"}
  ],
  temperature=0.3,
)

print(completion.choices[0].message)
```

#### AutoDev config

Engine Server:

```
https://api.moonshot.cn/v1/chat/completions 
```

Request body format

```json
{ "customFields": {"model": "moonshot-v1-8k"},   "messageKeys": {"role": "role", "content": "content"} }
```

Response format:

```
$.choices[0].delta.content 
```

## Custom Server API example (ChatGLM2)

See in: [ChatGLM2 SSE Server](../example/custom_llm_server/chatglm_sse.py)

Your LLM API example:

```http-request
POST http://127.0.0.1:8000/chat
Content-Type: application/json

{
  "messages": [
     { "role": "user", "message": "I'm Nihillum." },
     { "role": "assistant", "message": "OK" },
     { "role": "user", "message": "What did I just say?" }
  ]
}
```

Format Example

```python
class Message(BaseModel):
    role: str
    message: str


class ChatInput(BaseModel):
    messages: List[Message]


@app.post("/api/chat", response_class=Response)
async def chat(msg: ChatInput):
    return StreamingResponse(fetch_chat_completion(msg.messages), media_type="text/event-stream")
```
