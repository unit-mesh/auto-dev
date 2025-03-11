---
layout: default
title: AutoDev as MCP Client
nav_order: 2
parent: MCP
---


## How to use

1. Enable the MCP server in AutoDev settings
2. Use the MCP client to connect to the AutoDev server (We use JetBrains MCP Proxy Server to keep same protocol)

```json
{
  "mcpServers": {
    "AutoDev": {
      "command": "npx",
      "args": [
        "-y",
        "@jetbrains/mcp-proxy"
      ],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```


