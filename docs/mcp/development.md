---
layout: default
title: Develop MCP Server
nav_order: 3
parent: MCP
permalink: /mcp/development
---

## Build Server

Resources

- [MCP For Server Developers](https://modelcontextprotocol.io/quickstart/server)

Since MVP Specification has different version, we recommend you keep same with AutoDev

- Kotlin SDK: io.modelcontextprotocol:kotlin-sdk:0.4.0
- Java SDK: io.modelcontextprotocol.sdk:mcp:0.8.1


## Test Your Server with RPC Command

Start the server, and paste follow message:

```json
{"id":4,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"experimental":{},"sampling":{}},"clientInfo":{"name":"weather","version":"1.0.0"},"_meta":{},"method":"initialize"},"jsonrpc":"2.0"}
```

Success response example:

```json
{"jsonrpc":"2.0","id":4,"result":{"protocolVersion":"2024-11-05","capabilities":{"logging":{},"tools":{"listChanged":true}},"serverInfo":{"name":"my-weather-server","version":"0.0.1"}}}
```

Error response example:

```json
{"jsonrpc":"2.0","id":4,"error":{"code":-32603,"message":"Unrecognized field \"_meta\" (class io.modelcontextprotocol.spec.McpSchema$InitializeRequest), not marked as ignorable (3 known properties: \"protocolVersion\", \"clientInfo\", \"capabilities\"])\n at [Source: UNKNOWN; byte offset: #UNKNOWN] (through reference chain: io.modelcontextprotocol.spec.McpSchema$InitializeRequest[\"_meta\"])"}}
```