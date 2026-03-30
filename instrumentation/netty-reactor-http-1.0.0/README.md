# Reactor Netty HTTP 1.0.0 Instrumentation

Instrumentation for reactor-netty HTTP client (1.0.0+). This module provides external call reporting for **direct usage** of reactor-netty's `HttpClient` API.

## What This Module Does

This instrumentation captures HTTP external calls made via reactor-netty's `HttpClient` and reports them to New Relic with:
- **Library name**: "NettyReactor"
- **Operation**: HTTP method (GET, POST, PUT, DELETE, etc.)
- **Distributed tracing**: Adds outbound DT headers for cross-application tracing
- **Async support**: Uses token linking to preserve transaction context across NIO threads

## Version Coverage

This module instruments reactor-netty **1.0.0 and later**:
- **Supported**: `io.projectreactor.netty:reactor-netty-http:[1.0.0,)`
- **Artifact split**: reactor-netty 1.0+ split into `reactor-netty-http` + `reactor-netty-core`
- **Build config**: Uses `verifyClasspath = false` due to split artifacts

## When This Module Applies

### ✅ Activates For
- **Direct reactor-netty HttpClient usage**:
  ```java
  HttpClient client = HttpClient.create();
  client.get().uri("https://api.example.com").response().block();
  ```

### ❌ Does NOT Activate For
- **Spring WebClient**: Uses different reactor-netty hooks (not instrumented by this module)
- **RestTemplate with reactor-netty transport**: RestTemplate's `@Trace(leaf=true)` suppresses this module

This is **intentional design** - high-level APIs (Spring WebClient, RestTemplate) should report with their own library names, not "NettyReactor".

## Relationship to Other Modules

### netty-reactor-0.9.0 (and 0.7.0, 0.8.0)
These modules instrument **Reactor context propagation** (not HTTP external calls):
- **netty-reactor-0.9.0**: Ensures transaction context flows through Reactor chains
- **netty-reactor-http-1.0.0** (this module): Reports HTTP external calls

**Both modules load together** when reactor-netty 1.0+ is present:
- `netty-reactor-0.9.0` creates Reactor pipeline segments (e.g., `Mono.map`, `Flux.filter`)
- `netty-reactor-http-1.0.0` creates HTTP external call segments (e.g., `External/host/NettyReactor/GET`)

This is **complementary**, not duplicate data:
- **0.9.0**: "How did the transaction flow through the Reactor pipeline?"
- **http-1.0.0**: "What HTTP calls were made?"

## How It Works

### State-Based Instrumentation
Reactor-netty fires connection state change events. This instrumentation hooks into two states:

1. **`[request_prepared]` state**: Start segment, link token, add DT headers
2. **`[response_received]` state**: Report external call, end segment

```java
@Trace(async = true, excludeFromTransactionTrace = true)
public void onStateChange(Connection connection, State newState) {
    String state = newState.toString();

    if ("[request_prepared]".equals(state)) {
        // Link token for async continuation
        Token token = reactorContext.get("newrelic-token");
        if (token != null) token.link();

        // Start segment and store in WeakHashMap
        Segment segment = txn.startSegment("ReactorNettyHttpClient.request");
        ReactorNettyContext.put(connection, segment);
    }
    else if ("[response_received]".equals(state)) {
        // Retrieve segment from WeakHashMap
        SegmentData data = ReactorNettyContext.remove(connection);

        // Report external call
        data.segment.reportAsExternal(HttpParameters.library("NettyReactor")...);
        data.segment.end();
    }
}
```

### Critical Implementation Details
- **State string format**: Must use lowercase with brackets: `[request_prepared]`, `[response_received]` (NOT uppercase)
- **WeakHashMap storage**: `Connection → SegmentData` mapping 
- **Token linking**: Preserves transaction context across Netty NIO thread boundaries
- **Segment API**: `startSegment()` and `segment.end()` work across async boundaries

## Requirements

- **Java 17+**: reactor-netty 1.0+ requires Java 17 baseline
- **reactor-netty-http**: 1.0.0 or later
- **reactor-netty-core**: Transitive dependency (required by reactor-netty-http)

## Testing

**Note**: Unit tests for this module fail to capture external metrics due to suspected test framework limitations with Java 17 modules and async NIO threading.


## Example Usage

```java
public class MyService {
    public Mono<String> fetchData() {
        HttpClient client = HttpClient.create();

        return client.get()
            .uri("https://api.example.com/data")
            .responseContent()
            .aggregate()
            .asString();
    }
}
```

**In New Relic UI**:
- Transaction: `WebTransaction/Spring/MyService/fetchData` (if called from Spring controller)
- External call: `External/api.example.com/NettyReactor/GET`
- Library: "NettyReactor"
- Reactor segments: Created by `netty-reactor-0.9.0` (pipeline operations)

## Artifact Split (reactor-netty 1.0+)

reactor-netty 1.0 split into multiple Maven artifacts:
- `io.projectreactor.netty:reactor-netty-http` - HTTP client/server
- `io.projectreactor.netty:reactor-netty-core` - Core Netty integration

**Build configuration impact**:
```gradle
verifyInstrumentation {
    passes 'io.projectreactor.netty:reactor-netty-http:[1.0.0,)'
    verifyClasspath = false  // Skip artifact validation (both artifacts needed together)
}
```

Without `verifyClasspath = false`, verification would test each artifact separately (fail), but runtime has both (works).

## Changes from Earlier Versions

If maintaining this module, be aware:
- reactor-netty < 1.0.0 used different package structure (covered by `netty-reactor-0.x.0` modules)
- State string format is critical: `[request_prepared]` not `REQUEST_PREPARED`
- Use `WeakHashMap` for state storage
- Java 17 toolchain required for compilation
- Coexists with `netty-reactor-0.9.0` - both modules should load together