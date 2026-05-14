## MCP Java SDK Instrumentation

Instrumentation for the [Model Context Protocol (MCP) Java SDK](https://github.com/modelcontextprotocol/java-sdk)
(`io.modelcontextprotocol.sdk:mcp` 1.0.0+).

### What's Instrumented?
Both the synchronous and asynchronous MCP client APIs are instrumented:

| Class | Methods |
|-------|---------|
| `McpAsyncClient` | `callTool`, `readResource`, `getPrompt` |

Segments are only created when:
- An active New Relic transaction exists
- `ai_monitoring.enabled: true` is set in the agent config

### Span Naming

```
Llm/tool/MCP/call_tool/{tool_name}
Llm/resource/MCP/read_resource/{uri_scheme}
Llm/prompt/MCP/get_prompt/{prompt_name}
```

For resources, only the URI scheme is captured (e.g. `https`, `file`) — not the full path or query parameters.

### Sync vs Async

`McpSyncClient` is a blocking wrapper around `McpAsyncClient`. Only `McpAsyncClient` is instrumented, but sync calls are still covered since `McpSyncClient`
delegates internally to `McpAsyncClient`. 
