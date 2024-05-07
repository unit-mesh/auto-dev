---
layout: default
title: Custom LLM Server
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
- Customize the messages key via `messageKeys` (optional)

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

## Custom LLM Server Example

### Moonshot AI examples

- Custom Response Type：SSE
- Custom Engine Server：https://api.moonshot.cn/v1/chat/completions 
- Request body format
```json
{ "customFields": {"model": "moonshot-v1-8k", "stream": true } }
```
- Response format:
```
$.choices[0].delta.content
```

### DeepSeek AI examples

- Custom Response Type：SSE
- Custom Engine Server：https://api.deepseek.com/v1/chat/completions
- Request body format:
```json
{ "customFields": {"model": "deepseek-chat", "stream": true} }
```
- Response format: 
```
$.choices[0].delta.content 
```

### 零一万物 examples

- Custom Response Type：SSE
- Custom Engine Server：https://api.lingyiwangwu.com/v1/chat/completions
- Request body format:
```json
{ "customFields": {"model": "yi-34b-chat", "stream": true} }
```
- Response format: 
```
$.choices[0].delta.content 
```


### ChatGLM examples

more detail see in: [#90](https://github.com/unit-mesh/auto-dev/issues/90)

- Custom Response Type：SSE
- Custom Engine Server：https://open.bigmodel.cn/api/paas/v4/chat/completions
- Request body format:
```json
{ "customFields": {"model": "glm-4", "stream": true} }
```
- Response format: 

```
$.choices[0].delta.content 
```



