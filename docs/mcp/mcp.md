---
layout: default
title: MCP - AutoDev as MCP Service
nav_order: 6
has_children: true
permalink: /bridge
---

In issue [#330](https://github.com/unit-mesh/auto-dev/issues/330), we discussed the possibility of using AutoDev as a MCP server.
This document will discuss the implementation details.

## How to use

1. Enable the MCP server in AutoDev settings
2. Use the MCP client to connect to the AutoDev server (We use JetBrains MCP Proxy to keep same protocol)

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

## Supported Action (Todo)


