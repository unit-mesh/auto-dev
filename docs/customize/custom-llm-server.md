---
layout: default
title: Custom LLm Server
parent: Customize Features
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

Only support amount of request parameters like OpenAI does.
Only support http request that don't need encryption keys(like websocket)


### Custom Request (header and body)

You can add top level field to the request body,
And custom the origin keys for `role`, `messsage`

```json
{
  "customHeaders": { "my header": "my value" },
  "customFields": {"user": "userid", "date": "2012"},
  "messageKeys": {"role": "role", "content": "message"}
}
```

and the request body will be:

```json
{
	"user": "userid",
    "messages": [{"role": "user", "message": "..."}]
  }
```
