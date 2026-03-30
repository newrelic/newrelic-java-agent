# Spring RestTemplate 6.0.0 Instrumentation

Instrumentation for Spring Framework 6.x `RestTemplate` HTTP client. This module provides external call reporting and distributed tracing for RestTemplate operations in Spring 6.x / Spring Boot 3.x applications.

## What This Module Does

This instrumentation captures HTTP external calls made via Spring's `RestTemplate` API and reports them to New Relic with:
- **Library name**: "RestTemplate" (not the underlying transport library)
- **Operation**: HTTP method (GET, POST, PUT, DELETE, etc.)
- **Distributed tracing**: Adds outbound DT headers for cross-application tracing
- **Transport-agnostic**: Works with ANY HTTP client transport

## Version Coverage

This module instruments Spring Framework **6.x only**:
- **Supported**: Spring Framework 6.0.0 through 6.x
- **Not Supported**: Spring Framework 7.x+ (method signatures change)

## Transport-Agnostic Design

RestTemplate is a high-level HTTP client API that can use different underlying HTTP transports. This instrumentation reports **all requests as "RestTemplate"** regardless of the transport:

✅ **Supported Transports** (all report as "RestTemplate"):
- HttpURLConnection (JDK built-in, default)
- Apache HttpClient 5.x
- Jetty HttpClient
- OkHttp 3.x/4.x
- Reactor Netty (when used as RestTemplate transport)

This is **intentional design** - the library name "RestTemplate" indicates the API used, not the transport implementation. This prevents:
- Transport-level double reporting
- Inconsistent library names based on transport choice
- Confusion when transports are swapped

### How Transport Suppression Works

The instrumentation uses `@Trace(leaf=true)` on RestTemplate's `doExecute()` methods:

```java
@Trace(leaf = true)  // Suppresses child tracers
protected <T> T doExecute(URI url, HttpMethod method, ...) {
    T result = Weaver.callOriginal();

    NewRelic.getAgent().getTracedMethod().reportAsExternal(
        HttpParameters.library("RestTemplate")...
    );

    return result;
}
```

The `leaf=true` annotation:
1. Adds distributed tracing headers **before** calling the transport
2. Creates a leaf tracer that **suppresses all child tracers**
3. Reports the external call with "RestTemplate" library name
4. Prevents transport-level instrumentation (OkHttp, Apache, etc.) from creating duplicate externals

## Relationship to Other Modules


### Transport Instrumentation Modules
These modules **do NOT activate** when RestTemplate is used:
- `okhttp-4.0.0` - Suppressed by RestTemplate's `@Trace(leaf=true)`
- `httpclient-5.0` - Suppressed by RestTemplate's `@Trace(leaf=true)`
- `jetty-httpclient-9.4.0` - Suppressed by RestTemplate's `@Trace(leaf=true)`
- `netty-reactor-http-1.0.0` - Suppressed by RestTemplate's `@Trace(leaf=true)`

### Coexistence Pattern
When RestTemplate uses reactor-netty as transport:
1. RestTemplate instrumentation activates (reports as "RestTemplate")
2. `netty-reactor-http-1.0.0` instrumentation loads but doesn't execute
3. `@Trace(leaf=true)` prevents the reactor-netty instrumentation from creating segments
4. Result: **Single external call** reported as "RestTemplate"

## Requirements

- **Java 17+**: Spring 6.x requires Java 17 baseline
- **Spring Framework**: 6.0.0 through 6.x (7.x not supported)
- **Spring Boot**: 3.0+ (Spring Boot 3.x uses Spring Framework 6.x)

## Testing

**Note**: Unit tests for this module fail to capture external metrics due to suspected test framework limitations with Java 17 modules.

## Example Usage

```java
@RestController
public class MyController {

    @GetMapping("/fetch")
    public String fetchData() {
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject("https://api.example.com/data", String.class);
        return result;
    }
}
```

**In New Relic UI**:
- Transaction: `WebTransaction/SpringController/MyController/fetchData`
- External call: `External/api.example.com/RestTemplate/GET`
- Library: "RestTemplate" (regardless of actual HTTP transport used)

## Changes from Earlier Versions

If maintaining this module, be aware:
- Spring 6.x requires **both** 4-param and 5-param `doExecute()` instrumentation
- Spring 6.x calls can go through **either** method depending on the API used
- Version range must explicitly exclude Spring 7.x (method signatures change)
- Java 17 toolchain required for compilation