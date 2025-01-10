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

ps: For OpenAI compatible, you can use `Open AI Model` to your custom OpenAI compatible model.

### Custom Config / OpenAI compatible

Tested: 零一万物（[#94](https://github.com/unit-mesh/auto-dev/issues/94)）, 月之暗面（Moonshot
AI）、深度求索（Deepseek [#96](https://github.com/unit-mesh/auto-dev/issues/96)），ChatGLM(#90)

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `Custom`
3. fill `Custom Engine Server`
4. fill `Custom Engine Token` if needed.
5. config `Custom Response Format` by [JsonPath](https://github.com/json-path/JsonPath) (for
   example: `$.choices[0].content`), if not set, will use OpenAI's format as default.
6. config `Custom Request Format` by Json if needed.
7. Apply and OK.

for more, see in [Customize LLM Server](/custom/llm-server)
