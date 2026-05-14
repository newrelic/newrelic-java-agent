## MCP Java SDK Instrumentation

Instrumentation for the [Model Context Protocol (MCP) Java SDK](https://github.com/modelcontextprotocol/java-sdk)
(`io.modelcontextprotocol.sdk:mcp` 1.0.0+).

### What's Instrumented?
Both the synchronous and asynchronous MCP client APIs are instrumented:

| Class | Methods |
|-------|---------|
| `McpSyncClient` | `callTool`, `readResource`, `getPrompt` |
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

`McpSyncClient` is a blocking wrapper around `McpAsyncClient`. A sync call delegates
to the async client internally so both instrumentation classes would fire for a single
sync call and would incorrectly create two segments. The sync instrumentation sets `McpUtils.IN_SYNC_MCP_CALL`, a `ThreadLocal` flag,
before delegating to the async client. The async instrumentation checks this flag on entry
and skips segment creation if it is set, preventing a duplicate segment from being reported
for the same call.
