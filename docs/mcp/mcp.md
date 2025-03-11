---
layout: default
title: MCP
nav_order: 6
has_children: true
permalink: /mcp
---

# Model Context Protocol (MCP)

> MCP is an open protocol that standardizes how applications provide context to LLMs. Think of MCP like a USB-C port for
> AI applications. Just as USB-C provides a standardized way to connect your devices to various peripherals and
> accessories, MCP provides a standardized way to connect AI models to different data sources and tools.

In issue [#330](https://github.com/unit-mesh/auto-dev/issues/330), we discussed the possibility of using AutoDev as a
MCP server.
This document will discuss the implementation details.

Requirements: Install Node.js with Npx

- https://nodejs.org/en/download/package-manager

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

## Supported Action (Todo)


