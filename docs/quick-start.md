---
layout: default
title: Quick Start
nav_order: 2
permalink: /quick-start
---

# Usage

As we mentioned in LICENSE issue at the beginning of this project, JetBrains has reasonable grounds to consider 
the plugin to be the content infringing their own IP right.
So, since 2024.4, AutoDev was unavailable in the JetBrains Plugin Repository, you can download from 
[Releases](https://github.com/unit-mesh/auto-dev/releases)

~~1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)~~
1. Download from [Releases](https://github.com/unit-mesh/auto-dev/releases)
   - AutoDev-*-222.zip, for version 2022.2~2023.2
   - AutoDev-*-233.zip, for version 2023.3~2023.3
   - AutoDev-*-241.zip, for version 2024.1~
2. Configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `AutoDev`

### Custom Config / OpenAI compatible (Old before 2.0.0-beta.4)

Tested: 零一万物（[#94](https://github.com/unit-mesh/auto-dev/issues/94)）, 月之暗面（Moonshot
AI）、深度求索（Deepseek [#96](https://github.com/unit-mesh/auto-dev/issues/96)），ChatGLM(#90)

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. fill `LLM Server Address`, for example:
   - Deepseek: `https://api.deepseek.com/chat/completions`
   - OpenAI: `https://api.openai.com/v1/chat/completions`
3. fill `LLM Key` if needed, aka `API Key`.
4. config `Custom Response Format` by [JsonPath](https://github.com/json-path/JsonPath), for example
   - `$.choices[0].delta.content`
5. config `Custom Request Format`, for example:
   - `{ "customFields": {"model": "deepseek-chat", "stream": true }}`
6. Apply and OK.

for more, see in [Customize LLM Server](/custom/llm-server)

### New Config (2.0.0-beta.4+)

modelType: `["Default", "Plan", "Act", "Completion", "Embedding", "FastApply", "Others"]`

- Default: the default model for all cases if not specified
- Plan: for reasoning, planning, etc, like: `DeepSeek R1`, recommend to use Best Model
- Act: for action, like: `DeepSeek V3`, `Qwen 72B` etc
- Completion: for code completion, not support FIM yet.
- Embedding: for embedding, like: `sentence-transformers/all-MiniLM-L6-v2`
- FastApply: for fix patch generate issue, like: `Kortix/FastApply-1.5B-v1.0`
- Others: just a placeholder, no special treatment

Examples:

```json
[
  {
    "name": "GLM4-Plus",
    "url": "https://open.bigmodel.cn/api/paas/v4/chat/completions",
    "auth": {
      "type": "Bearer",
      "token": "sk-ii"
    },
    "requestFormat": "{ \"customFields\": {\"model\": \"glm-4-plus\", \"stream\": true}}",
    "responseFormat": "$.choices[0].delta.content",
    "modelType": "FastApply"
  },
  {
    "name": "DeepSeek R1",
    "url": "https://api.deepseek.com/chat/completions",
    "auth": {
      "type": "Bearer",
      "token": "sk-ii"
    },
    "requestFormat": "{ \"customFields\": {\"model\": \"deepseek-reasoner\", \"stream\": true}}",
    "responseFormat": "$.choices[0].delta.content",
    "modelType": "Plan"
  },
  {
     "name": "DifyAI",
     "description": "Dify Example",
     "url": "https://api.dify.ai/v1/completion-messages",
     "auth": {
        "type": "Bearer",
        "token": "app-abcd"
     },
     "requestFormat": "{\"fields\": {\"inputs\": {\"feature\": \"$content\"}, \"response_mode\": \"streaming\", \"user\": \"phodal\" }}",
     "responseFormat": "$.answer",
     "modelType": "Others"
  }
]
```

- URL: the LLM Server Address with `/chat/completions`
- Auth: the auth info, `Bearer` only, `token` is the API Key
- RequestFormat: the request format, like: `{"customFields": {"model": "deepseek-chat", "stream": true }}`
- ResponseFormat: the response format, like: `$.choices[0].delta.content`
- ModelType: the model type, see above
