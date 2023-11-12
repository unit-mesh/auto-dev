---
layout: default
title: Custom LLm Server
parent: Customize
nav_order: 14
permalink: /custom/llm-server
---

# Custom Server API example (ChatGLM2)

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
