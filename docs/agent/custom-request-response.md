---
layout: default
title: Custom Request/Response
parent: Agent
nav_order: 5
---

# Custom Request/Response

the Custom Request/Response is similar to [Custom LLM server](/custom/llm-server), which defines the request and 
response format under the `connector` field.

```json
{
  "name": "内部 API 集成",
  "url": "http://127.0.0.1:8765/api/agent/api-market",
  "auth": {
    "type": "Bearer",
    "token": "eyJhbGci"
  },
  "connector": {
    "requestFormat": "{\"customFields\": {\"model\": \"yi-34b-chat\", \"stream\": true}}",
    "responseFormat": "$.choices[0].delta.content"
  },
  "responseAction": "Direct",
  "interactive": "ChatPanel"
}
```

## Advanced example

We used [JsonPath](https://github.com/json-path/JsonPath) to parse response, If you want to get stream content from Coze
API, you can use `$[?(@.type == 'answer' && !(@.created_at))].content` to filter the content.

Here some examples of the request and response format:

```bash
event:conversation.message.delta
data:{"id":"7490890868547862564","conversation_id":"7490890868547682340","bot_id":"7490858512726458380","role":"assistant","type":"answer","content":"All","content_type":"text","chat_id":"7490890868547698724","section_id":"7490890868547682340"}

event:conversation.message.delta
data:{"id":"7490890868547862564","conversation_id":"7490890868547682340","bot_id":"7490858512726458380","role":"assistant","type":"answer","content":"Files","content_type":"text","chat_id":"7490890868547698724","section_id":"7490890868547682340"}

event:conversation.message.completed
data:{"id":"7490890868547862564","conversation_id":"7490890868547682340","bot_id":"7490858512726458380","role":"assistant","type":"answer","content":"listAllFiles","content_type":"text","chat_id":"7490890868547698724","section_id":"7490890868547682340","created_at":1744108946}

event:conversation.message.completed
data:{"id":"7490890877657923603","conversation_id":"7490890868547682340","bot_id":"7490858512726458380","role":"assistant","type":"verbose","content":"{\"msg_type\":\"generate_answer_finish\",\"data\":\"{\\\"finish_reason\\\":0,\\\"FinData\\\":\\\"\\\"}\",\"from_module\":null,\"from_unit\":null}","content_type":"text","chat_id":"7490890868547698724","section_id":"7490890868547682340","created_at":1744108948,"updated_at":1744108948}
```

explain:

1. `answer` is the type of the response, which is the content you want to get.
2. `created_at` is the time when the response is completed, which is not needed in the response.
