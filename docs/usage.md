---
layout: default
title: Quick Start
nav_order: 2
permalink: /quick-start
---

# Usage

1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
2. Configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `AutoDev`

## Config

### OpenAI Official config

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `OpenAI`, select `Open AI Model` -> `gpt-3.5-turbo`
3. fill `OpenAI API Key` with your OpenAI API Key.
4. Apply and OK.

### OpenAI Proxy

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `OpenAI`, select `Open AI Model` -> `gpt-3.5-turbo`,
3. fill `Custom OpenAI Host` with your OpenAI API Endpoint.
4. fill `OpenAI API Key` with your Host OpenAI API Key.
5. Apply and OK.

### OpenAI Azure Config

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `Azure`, select `Open AI Model` -> `gpt-3.5-turbo`
3. fill `Custom OpenAI Host` with your OpenAI API Endpoint. (with Key in URL)
4. Apply and OK.

### Xunfei Xinghuo

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `Xinghuo`
3. fill `AppId`, `AppKey`, `AppSecret` with your Xunfei Xinghuo API Key.
4. Apply and OK.

### Custom Config / OpenAI compatible

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `Custom`
3. fill `Custom Engine Server`
4. fill `Custom Engine Token` if needed.
5. config `Custom Response Format` by [JsonPath](https://github.com/json-path/JsonPath) (for example: `$.choices[0].content`), if not set, will use OpenAI's format as default.
6. Apply and OK.

for more, see in [Customize LLM Server](/custom/llm-server)
