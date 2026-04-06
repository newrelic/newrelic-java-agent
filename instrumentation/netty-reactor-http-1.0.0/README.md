# Reactor Netty HTTP 1.0.0 Instrumentation

Instrumentation for reactor-netty HTTP client (1.0.0+). This module provides external call reporting for **direct usage** of reactor-netty's `HttpClient` API.

### What This Module Does

This instrumentation captures HTTP external calls made via reactor-netty's `HttpClient` and reports them to New Relic with:
- **Library name**: "NettyReactor"
- **Operation**: HTTP method (GET, POST, PUT, DELETE, etc.)
- **Distributed tracing**: Adds outbound DT headers for cross-application tracing
- **Async support**: Uses token linking to preserve transaction context across NIO threads

### Version Coverage

This module instruments reactor-netty **1.0.0 and later**:
- **Supported**: `io.projectreactor.netty:reactor-netty-http:[1.0.0,)`
- **Artifact split**: reactor-netty 1.0+ split into `reactor-netty-http` + `reactor-netty-core`
- **Build config**: Uses `verifyClasspath = false` due to split artifacts

### When This Module Applies

- **Activates For:**
  - Direct reactor-netty HttpClient usage

- **Does NOT Activate For:**
  - Spring WebClient: Uses different reactor-netty hooks (not instrumented by this module)
  - RestTemplate with reactor-netty transport: RestTemplate's `@Trace(leaf=true)` suppresses this module

This is **intentional design** - high-level APIs (Spring WebClient, RestTemplate) should report with their own library names, not "NettyReactor".

### Relationship to netty-reactor-0.9.0 (and 0.7.0, 0.8.0)
- These modules instrument **Reactor context propagation** (not HTTP external calls):
  - **netty-reactor-0.9.0**: Ensures transaction context flows through Reactor chains
  - **netty-reactor-http-1.0.0** (this module): Reports HTTP external calls
- **Both modules load together** when reactor-netty 1.0+ is present:
  - `netty-reactor-0.9.0` creates Reactor pipeline segments (e.g., `Mono.map`, `Flux.filter`)
  - `netty-reactor-http-1.0.0` creates HTTP external call segments (e.g., `External/host/NettyReactor/GET`)
  - **netty-reactor-http-1.0.0** provides complementary, not duplicate data

### Works Via State-Based Instrumentation
Reactor-netty fires connection state change events. This instrumentation hooks into two states:

1. **`HttpClientState.REQUEST_PREPARED`**: Start segment, link token, add DT headers
2. **`HttpClientState.RESPONSE_RECEIVED`**: Report external call, end segment


### Implementation Details
- **State comparison**: Uses enum constants `HttpClientState.REQUEST_PREPARED` and `HttpClientState.RESPONSE_RECEIVED`
- **Type checking**: Verifies connection type (`HttpClientRequest` or `HttpClientResponse`) before casting
- **WeakHashMap storage**: `Connection → SegmentData` segment data removed when response received
- **Token linking**: Preserves transaction context across Netty NIO thread boundaries
- **Segment API**: `startSegment()` and `segment.end()` work across async boundaries

### Requirements

- **Java 17+**: reactor-netty 1.0+ requires Java 17 baseline
- **reactor-netty-http**: 1.0.0 or later
- **reactor-netty-core**: Transitive dependency (required by reactor-netty-http)

### Testing

**Note**: Tests for this module fail to capture external metrics due to suspected test framework limitations with Java 17 modules and async NIO threading.

### Maintenance Notes
- reactor-netty < 1.0.0 used different package structure (covered by `netty-reactor-0.x.0` modules)
- Uses `WeakHashMap` so segment data is removed when response received
- Java 17 toolchain required for compilation
- Coexists with `netty-reactor-0.9.0` - both modules should load together