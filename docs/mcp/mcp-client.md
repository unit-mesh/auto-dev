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

### MCP as DevIns

In AutoDev, the MCP tool will be converted to DevIns instruction. For example, the `read_multiple_files` tool will be converted to:

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

### Test for Sketch call

- http://127.0.0.1:63342/api/mcp/list_tools


```bash
➜  ~ curl -X POST "http://127.0.0.1:63343/api/mcp/issue_or_story_evaluate" \
>      -H "Content-Type: application/json" \
>      -d '{"issue": "重构 blog 为 DDD 服务"}'
{
    "error": "Failed to execute tool issue_or_story_evaluate, message Access is allowed from Event Dispatch Thread (EDT) only; see https://jb.gg/ij-platform-threading for details\nCurrent thread: Thread[Netty Builtin Server 3,5,main] 496134154 (EventQueue.isDispatchThread()=false)\nSystemEventQueueThread: Thread[AWT-EventQueue-0,6,main] 321680684"
}
```
