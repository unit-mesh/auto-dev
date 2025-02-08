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

## Config

### Custom Config / OpenAI compatible

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
