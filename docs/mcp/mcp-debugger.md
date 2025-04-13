---
layout: default
title: MCP Debugger
nav_order: 4
parent: MCP
permalink: /mcp/mcp-debugger
---

## AutoDev MCP debugger

<img src="https://unitmesh.cc/auto-dev/mcp-debugger.png" alt="Inline Chat" width="600px"/>

create a file end with `.mcp.json` in your project root directory, and add the following content:

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "/Volumes/source/ai/autocrud"
      ]
    }
  }
}
```

Then, click show preview in File toolbar, you can see:

- MCP Server/Tool LIST
- MCP Model Configured
- MCP Chat input box

After send a message, you can see the response in the console:

<img src="https://unitmesh.cc/auto-dev/mcp-debugger-chat.png" alt="Inline Chat" width="600px"/>

