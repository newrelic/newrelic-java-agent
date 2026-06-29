# Reactor Netty HTTP 1.0.0 Instrumentation

External call reporting for HTTP calls flowing through `reactor.netty.http.client.HttpClient` (1.0.0+).

## Output

- Library: `NettyReactor`
- Metric shape: `External/<host>/NettyReactor/<METHOD>`
- Distributed tracing: outbound DT headers added before the request flushes

## When It Activates

Fires for **direct usage of `reactor.netty.http.client.HttpClient`** code that builds an `HttpClient` itself and calls `.get()` / `.post()` / etc.

Suppressed by `spring-resttemplate-6.0.0` when `RestTemplate` uses a reactor-netty transport (`@Trace(leaf=true)` makes the call report as `RestTemplate`).

## Coexistence With `netty-reactor-0.9.0` (and 0.7.0 / 0.8.0)

The other modules instrument `TokenLinkingSubscriber` for Reactor context propagation. This module instruments `HttpClientConnect$HttpIOHandlerObserver` for external call reporting. They load together when reactor-netty 1.0+ is present and produce complementary data.

## How It Works

Hooks `HttpClientConnect$HttpIOHandlerObserver.onStateChange()`. Reacts to:

- `REQUEST_PREPARED` → token-link, start segment, add DT headers
- `RESPONSE_RECEIVED` → report external call, end segment
- `RESPONSE_COMPLETED` / `DISCONNECTING` / `RELEASED` / `onUncaughtException` → defensive cleanup of any segment not already ended via `RESPONSE_RECEIVED`.

## Requirements

- Java 17+ (reactor-netty 1.0+ requires it)

## Tests

- **Unit tests** (`ReactorNettyHelperTest`): `./gradlew :instrumentation:netty-reactor-http-1.0.0:test`
- `InstrumentationTestRunner` cannot capture external metrics for this module (possible Java 17 and async NIO limitation)