---
layout: default
title: AutoDev as MCP Client
nav_order: 2
parent: MCP
---

## How to use

1. Configure the MCP client in `Settings`, `AutoDev`, `Custom Agent` MCP Servers
2. Add your MCP Server, for example:

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

Java examples:

```json
{
  "mcpServers": {
    "echo": {
      "command": "uv",
      "args": [
        "run",
        "--with",
        "mcp",
        "mcp",
        "run",
        "/Users/phodal/source/ai/autodev-mcp-test/python-sqlite3/server.py"
      ]
    },
    "weather": {
      "command": "java",
      "args": [
        "-jar",
        "/Volumes/source/ai/autodev-mcp-test/kotlin-weather-stdio-server/build/libs/weather-stdio-server-0.1.0-all.jar"
      ]
    }
  }
}
```

### MCP as DevIns

In AutoDev, the MCP tool will be converted to DevIns instruction. For example, the `read_multiple_files` tool will be
converted to:

```xml

<tool>name: read_multiple_files, desc: Read the contents of multiple files simultaneously. This is more efficient than
    reading files one by one when you need to analyze or compare multiple files. Each file's content is returned with
    its path as a reference. Failed reads for individual files won't stop the entire operation. Only works within
    allowed directories., example:
    <devin>
        Here is command and JSON schema
        /read_multiple_files
        ```json
        {"properties":{"paths":{"type":"array","items":{"type":"string"}}},"required":["paths"]}
        ```
    </devin>
</tool>
```

then Sketch, Bridge agent can use in the DevIns instruction.

### Test MCP Server

Create a new `sample.devin` file with the following content:

     /list_directory
     ```json
     {
      "path": "/Volumes/source/ai/autocrud/docs/mcp"
     }
     ```

Then run the following command, will return the list of files in the directory:

```bash
Execute list_directory tool's result
[
    {
        "text": "[FILE] mcp-client.md\n[FILE] mcp-server.md\n[FILE] mcp.md"
    }
]
```
