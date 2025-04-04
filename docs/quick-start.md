---
layout: default
title: Quick Start
nav_order: 2
permalink: /quick-start
---

# Installation and Setup

## Option 1: JetBrains Marketplace for 241+ (2024.1+)

Install directly from JetBrains Marketplace Repository: [AutoDev Sketch](https://plugins.jetbrains.com/plugin/26988-autodev-sketch)

Notes: the plugin is named `AutoDev Sketch` in the JetBrains Marketplace, and the id is `com.unitmesh.autodev`. It's
different from the one in the custom repository and GitHub releases.

## Option 2: Custom Repository

1. Go to `Settings` → `Plugins` → `Marketplace` → `Manage Plugin Repositories`
2. Add the following URL:
   ```
   https://plugin.unitmesh.cc/updatePlugins.xml
   ```

## Option 3: GitHub Releases

1. Download the appropriate version from [GitHub Releases](https://github.com/unit-mesh/auto-dev/releases)
   - AutoDev-*-222.zip — For versions 2022.2 to 2023.2
   - AutoDev-*-233.zip — For version 2023.3
   - AutoDev-*-241.zip — For version 2024.1 and newer
2. Install the plugin from disk in the JetBrains IDE

## Configuration

After installation, configure the plugin in `Settings` → `Tools` → `AutoDev`

### Legacy Configuration (Before 2.0.0-beta.4)

Supported providers: 零一万物（[#94](https://github.com/unit-mesh/auto-dev/issues/94)）, Moonshot AI, Deepseek ([#96](https://github.com/unit-mesh/auto-dev/issues/96)), ChatGLM(#90)

1. Open AutoDev Config in `Settings` → `Tools` → `AutoDev`
2. Configure `LLM Server Address`, examples:
   - Deepseek: `https://api.deepseek.com/chat/completions`
   - OpenAI: `https://api.openai.com/v1/chat/completions`
3. Enter your `LLM Key` (API Key)
4. Set `Custom Response Format` using [JsonPath](https://github.com/json-path/JsonPath), example:
   - `$.choices[0].delta.content`
5. Configure `Custom Request Format`, example:
   - `{ "customFields": {"model": "deepseek-chat", "stream": true }}`

For more details, see [Customize LLM Server](/custom/llm-server)

### Current Configuration (2.0.0-beta.4+)

Available model types:
- `Default`: Used for all cases if not specified
- `Plan`: For reasoning and planning (recommended: DeepSeek R1)
- `Act`: For action execution (e.g., DeepSeek V3, Qwen 72B)
- `Completion`: For code completion
- `Embedding`: For embedding functions (e.g., sentence-transformers/all-MiniLM-L6-v2)
- `FastApply`: For fix patch generation (e.g., Kortix/FastApply-1.5B-v1.0)
- `Others`: Generic placeholder

Configuration examples:

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

Configuration fields:
- **URL**: LLM Server Address with endpoint path
- **Auth**: Authentication information (currently Bearer token only)
- **RequestFormat**: JSON structure for API requests
- **ResponseFormat**: JsonPath to extract content from responses
- **ModelType**: Type of model (see list above)
