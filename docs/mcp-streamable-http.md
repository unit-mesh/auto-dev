# MCP Transport Support

AutoDev now supports MCP (Model Context Protocol) servers via multiple transport protocols, thanks to the upgrade to kotlin-sdk 0.6.0.

## Configuration

MCP servers can be configured in your `mcp_server.json` file using any of these transport types:

1. **Command-based (stdio)** - for local MCP servers
2. **URL-based (HTTP)** - for remote MCP servers with HTTP transport
3. **SSE URL-based (Server-Sent Events)** - for remote MCP servers with SSE transport

### Stdio Transport (Local Servers)

```json
{
  "mcpServers": {
    "local-server": {
      "command": "npx",
      "args": ["@modelcontextprotocol/server-stdio"],
      "env": {
        "API_KEY": "your-api-key"
      }
    }
  }
}
```

### HTTP Transport (Remote Servers)

```json
{
  "mcpServers": {
    "remote-server": {
      "url": "http://localhost:8080/mcp",
      "args": []
    }
  }
}
```

### SSE Transport (Server-Sent Events)

```json
{
  "mcpServers": {
    "sse-server": {
      "sseUrl": "http://localhost:8080/sse",
      "args": []
    }
  }
}
```

### Mixed Configuration

You can use all transport types in the same configuration:

```json
{
  "mcpServers": {
    "local-github": {
      "command": "/path/to/github-mcp-server",
      "args": ["stdio"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "ghp_xxxxxxxxxxxx"
      }
    },
    "remote-http-api": {
      "url": "https://api.example.com/mcp",
      "args": [],
      "autoApprove": ["safe_tool"],
      "requiresConfirmation": ["dangerous_tool"]
    },
    "remote-sse-api": {
      "sseUrl": "https://api.example.com/mcp/sse?session=123",
      "args": [],
      "autoApprove": ["read_only_tool"]
    }
  }
}
```

## Configuration Fields

### Required Fields

- **At least one** of `command`, `url`, or `sseUrl` must be specified
- `args`: Array of arguments (can be empty for HTTP/SSE servers)

### Optional Fields

- `disabled`: Boolean to disable the server
- `autoApprove`: Array of tool names that don't require confirmation
- `env`: Environment variables (only used with stdio transport)
- `requiresConfirmation`: Array of tool names that require explicit confirmation

## Transport Selection Logic

AutoDev automatically selects the appropriate transport based on your configuration (in priority order):

1. If `sseUrl` is provided → Uses `SseClientTransport` (highest priority)
2. If `url` is provided → Uses `StreamableHttpClientTransport`
3. If `command` is provided → Uses `StdioClientTransport`
4. If none are provided → Logs warning and skips the server

**Note**: If multiple transport options are specified, SSE has the highest priority, followed by HTTP, then stdio.

## Benefits of Remote Transports

### HTTP Transport
- **Remote Access**: Connect to MCP servers running on different machines
- **Scalability**: Better for production deployments
- **Firewall Friendly**: Uses standard HTTP protocols
- **Load Balancing**: Can be used with load balancers and reverse proxies

### SSE Transport
- **Real-time Communication**: Server-Sent Events provide efficient streaming
- **Low Latency**: Optimized for real-time data streaming
- **Browser Compatible**: Standard web technology
- **Connection Persistence**: Maintains long-lived connections for better performance

## Migration from stdio-only

Existing configurations with `command` will continue to work unchanged. To migrate a server to remote transports:

**Before (stdio):**
```json
{
  "my-server": {
    "command": "node",
    "args": ["server.js"]
  }
}
```

**After (HTTP):**
```json
{
  "my-server": {
    "url": "http://localhost:3000/mcp",
    "args": []
  }
}
```

**After (SSE):**
```json
{
  "my-server": {
    "sseUrl": "http://localhost:3000/sse",
    "args": []
  }
}
```

## Troubleshooting

### Connection Issues

- Verify the HTTP/SSE server is running and accessible
- Check firewall settings for HTTP connections
- Ensure the MCP server supports the appropriate transport protocol (HTTP or SSE)
- For SSE: Verify the server supports Server-Sent Events and the endpoint is correct

### Configuration Errors

- Validate JSON syntax in your configuration file
- Ensure at least one of `command`, `url`, or `sseUrl` is specified for each server
- Check server logs for detailed error messages

## Examples

See `example/mcp/streamable-http-example.mcp.json` for a complete configuration example with all transport types (stdio, HTTP, and SSE).
