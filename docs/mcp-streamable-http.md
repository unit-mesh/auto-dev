# MCP HTTP Transport Support

AutoDev now supports MCP (Model Context Protocol) servers via HTTP transport, thanks to the upgrade to kotlin-sdk 0.6.0.

## Configuration

MCP servers can be configured in your `mcp_server.json` file using either of these transport types:

1. **Command-based (stdio)** - for local MCP servers
2. **URL-based (HTTP)** - for remote MCP servers with HTTP transport

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



### Mixed Configuration

You can use both transport types in the same configuration:

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
    }
  }
}
```

## Configuration Fields

### Required Fields

- **Either** `command` or `url` must be specified
- `args`: Array of arguments (can be empty for HTTP servers)

### Optional Fields

- `disabled`: Boolean to disable the server
- `autoApprove`: Array of tool names that don't require confirmation
- `env`: Environment variables (only used with stdio transport)
- `requiresConfirmation`: Array of tool names that require explicit confirmation

## Transport Selection Logic

AutoDev automatically selects the appropriate transport based on your configuration:

1. If `url` is provided → Uses `StreamableHttpClientTransport`
2. If `command` is provided → Uses `StdioClientTransport`
3. If neither is provided → Logs warning and skips the server

**Note**: If both transport options are specified, HTTP has priority over stdio.

## Benefits of HTTP Transport

- **Remote Access**: Connect to MCP servers running on different machines
- **Scalability**: Better for production deployments
- **Firewall Friendly**: Uses standard HTTP protocols
- **Load Balancing**: Can be used with load balancers and reverse proxies

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



## Troubleshooting

### Connection Issues

- Verify the HTTP server is running and accessible
- Check firewall settings for HTTP connections
- Ensure the MCP server supports the HTTP transport protocol

### Configuration Errors

- Validate JSON syntax in your configuration file
- Ensure either `command` or `url` is specified for each server
- Check server logs for detailed error messages

## Examples

See `example/mcp/streamable-http-example.mcp.json` for a complete configuration example with both transport types (stdio and HTTP).
