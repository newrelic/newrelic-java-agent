# Spring RestTemplate 6.0.0 Instrumentation

Instrumentation for Spring Framework 6.x RestTemplate HTTP client. This module provides external call reporting and distributed tracing for RestTemplate operations in Spring 6.x / Spring Boot 3.x applications.

### What This Module Does

This instrumentation captures HTTP external calls made via Spring's RestTemplate API and reports them to New Relic with:
- **Library name**: "RestTemplate" (not the underlying transport library)
- **Operation**: HTTP method (GET, POST, PUT, DELETE, etc.)
- **Distributed tracing**: Adds outbound and inbound DT headers for cross-application tracing
- **Transport-agnostic**: Works with ANY HTTP client transport

### Version Coverage

This module instruments Spring Framework **6.x only**:
- **Supported**: Spring Framework 6.0.0 through 6.x
- **Not Supported**: Spring Framework 7.x+ (method signatures change)

### Transport-Agnostic Design

This instrumentation reports all requests as `RestTemplate` regardless of the underlying HTTP transport (HttpURLConnection, Apache HttpClient, Jetty, OkHttp, Reactor Netty). This prevents transport-level double reporting and ensures consistent library naming.

Transport instrumentation modules (`okhttp-4.0.0`, `httpclient-5.0`, `jetty-httpclient-9.4.0`, `netty-reactor-http-1.0.0`) do not activate when RestTemplate is used. The `@Trace(leaf=true)` annotation suppresses transport-level instrumentation, ensuring a single external call is reported as "RestTemplate".

### Requirements

- **Java 17+**: Required by Spring Framework 6.x
- **Spring Framework**: 6.0.0 through 6.x

### Testing

Tests for this module fail to capture external metrics due to suspected test framework limitations with Java 17 modules.

### Maintenance Notes

- Spring 6.x requires **both** 4-param and 5-param `doExecute()` instrumentation
- Spring 6.x calls can go through **either** method depending on the API used