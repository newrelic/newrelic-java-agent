# OTLP Export for Log Events

The Java agent can export `LogEvent` data over **OTLP/HTTP with binary protobuf encoding**. This works with New Relic's OTLP endpoint.

- **Logs**: sent via OTLP *instead of* to the collector (`log_event_data`) endpoint.
- **Attribute names**: New Relic names are kept, not translated to OTel semantic conventions, so the New Relic UI keeps working.

## Why this approach

Three options were evaluated for building the payload:

1. A handwritten OTLP protobuf encoder.
2. Reusing the OpenTelemetry Java SDK exporters.
3. OTLP/JSON on the agent's existing JSON library.

The SDK exporters were rejected for these reasons:
- **Size**: ~1.4 MB of OTel jars, or ~4.4 MB with okhttp/okio/kotlin.
- **Copies**: every harvested event would need converting to `LogRecordData`.
- **Unstable APIs**: the marshalers and senders are in `*.internal` packages.
- **Shading**: ServiceLoader lookups, a multi-release jar, `GlobalOpenTelemetry` self-metrics, and a second HTTP stack with its own proxy/TLS setup.

The current encoder builds the payload with the real generated protobuf message classes
(`io.opentelemetry.proto:opentelemetry-proto`), not a hand-rolled wire-format writer, and not the OTel SDK/exporter:
- **Not the rejected option**: `opentelemetry-proto` is pure generated message code with no OTel SDK/exporter — no
  ServiceLoader, no `GlobalOpenTelemetry`, no second HTTP stack. (The OTel SDK/exporter itself doesn't even use
  protobuf-java; it has its own hand-rolled marshaler.)
- **No new runtime dependency, in practice**: the agent already shades `com.google.protobuf:protobuf-java:3.25.5` and
  `io.grpc:*` into `newrelic-agent.jar` via `infinite-tracing` → `infinite-tracing-protobuf` (Infinite Tracing's
  gRPC span-streaming client). `opentelemetry-proto` is pinned to `1.0.0-alpha`, the last release generated against
  that pre-4.x protobuf-java codegen style, specifically so it shares that already-shaded protobuf-java runtime
  instead of pulling in a second, newer copy — a newer `opentelemetry-proto` release requires protobuf-java 4.x,
  which would both add real size and couple two independently-versioned dependencies at the class-load level
  (newer protobuf-java gencode throws at static-init if the runtime version drifts too far from what it was
  generated against). The net new footprint of staying on `1.0.0-alpha` is just that one ~650 KB jar.
- **Transport unchanged**: reuses the agent's existing Apache HTTP client, so the agent's proxy, `ca_bundle_path`
  and timeout settings still apply.
- **Spec stability**: the OTLP log protos it covers (`LogRecord`, `Resource`, `InstrumentationScope`, `KeyValue`/
  `AnyValue`) are marked Stable and have been unchanged since `1.0.0-alpha`, so pinning to that old release costs
  nothing functionally.
- **Trade-off**: this still promotes `opentelemetry-proto`/`protobuf-java` from a test-only dependency to a
  production one, which is a one-way door — any future bump of either `infinite-tracing-protobuf`'s or this
  module's protobuf dependency needs to be checked against the other. Hand-rolled wire-format code (and its test
  coverage) is deleted in exchange, removing the risk of a hand-rolled encoding bug.

### Endpoint resolution

- `/v1/logs` is appended to the base `host` endpoint configured for the agent.
- If the base `endpoint` isn't set, it's derived from the license key region, using the same logic as the collector host:
  - `https://collector.newrelic.com` when the license key has no region prefix.
  - `https://collector.<region>.nr-data.net` otherwise, e.g. `collector.eu01.nr-data.net`.
- [FedRAMP](https://docs.newrelic.com/docs/security/security-privacy/compliance/fedramp-compliant-endpoints/) (`https://gov-collector.newrelic.com`) doesn't follow that host pattern, so it must be configured explicitly. 

### Headers and authentication

- **The license key is sent only to New Relic endpoints.** The `api-key: <license key>` header is added automatically only when the endpoint host ends in `newrelic.com` or `.nr-data.net`, and only if no `api-key` header is configured. For any other host it's never sent, and the agent logs an INFO message.

### Other behavior

- **Serverless mode**: OTLP export is disabled, with a warning.
- **Startup failure**: if the sender can't be created (e.g. an invalid URL), the agent logs a warning and falls back to collector-only behavior.
- **Before connect**: like the collector path, data is only sent once the agent has connected, so the entity GUID and resource attributes are known.

## Wire format and mapping

Each harvest sends one gzip-compressed `ExportLogsServiceRequest`:
- `Content-Type: application/x-protobuf`
- `Content-Encoding: gzip`
- POST to the signal's endpoint.

**Resource and scope**
- Resource: `service.name` = app name, plus any other resource attributes that should be included.
- Instrumentation scope: `newrelic-java-agent` and the agent version.

### LogEvent → LogRecord

| LogRecord field | Source |
|---|---|
| `time_unix_nano` | `timestamp` attribute. Epoch ms; values above 10^15 are treated as ns, which is what the OTel SDK bridge records. Falls back to the observed time. |
| `observed_time_unix_nano` | When the agent created the `LogEvent` |
| `severity_text` | `level` |
| `severity_number` | Mapped from `level`: TRACE/FINEST/FINER→1, DEBUG/FINE/CONFIG→5, INFO→9, WARN/WARNING→13, ERROR/SEVERE→17, FATAL→21, otherwise unset |
| `body` | `message` |
| `trace_id` / `span_id` | `trace.id` / `span.id` (hex → bytes) |
| `attributes` | Every other attribute under its New Relic name, e.g. `level`, `entity.guid`, `entity.name`, `hostname`, `thread.*`, `logger.*`, `error.*`, `context.*`, `tags.*`, `k8s.*` |

**Attribute values**
- String, Boolean, Integer/Long/Short/Byte and Float/Double map to the matching `AnyValue` type.
- Anything else is written as a string, the same as in the collector JSON.

## Error handling

| Situation | Behavior |
|---|---|
| Compressed payload ≥ 1,000,000 bytes (the New Relic limit) | Dropped. A WARNING is logged, `Supportability/Java/OTLP/MaxPayloadSizeLimit/<signal>` is incremented, and `MaxPayloadException` is thrown. |
| HTTP 200 / 202 | Success |
| Any other status | `HttpError`. The agent's existing retry rules apply: 408/429/500/503 keep the batch for the next harvest; everything else discards it. |
| Logs: retryable error | Rethrown, so `LogSenderServiceImpl` merges the batch into the next harvest |

## Supportability metrics

| Metric | Meaning |
|---|---|
| `Supportability/Java/OTLP/HttpCode/{code}` | Response codes |
| `Supportability/Java/OTLP/{signal}/Duration` | Request duration |
| `Supportability/Java/OTLP/MaxPayloadSizeLimit/{signal}` | Payloads dropped for size |
| `Supportability/Java/OTLP/Output/Bytes` | Data usage, uncompressed payload bytes, matching the collector's data usage metrics |
| `Supportability/Java/OTLP/{signal}/Output/Bytes` | Data usage per signal, uncompressed payload bytes |

`{signal}` is `Logs`.

In audit mode, each OTLP request is logged with its URL, event count, compressed size and response code. The protobuf body itself isn't logged.

## Files changed

### New: `newrelic-agent/src/main/java/com/newrelic/agent/transport/otlp/`

| File | Purpose |
|---|---|
| `OtlpAttributes.java` | Shared builders for `KeyValue`/`AnyValue`, `Resource` and `InstrumentationScope` (using the generated `io.opentelemetry.proto` classes), plus hex ID → bytes conversion |
| `OtlpLogEncoder.java` | Builds `ExportLogsServiceRequest` from `LogEvent`s using the generated protobuf builders |
| `OtlpDataSender.java` | Handles gzip, the size check, per-signal URLs and headers, the license key host check, the HTTP POST, status handling and metrics |

### Modified

| File | Change |
|---|---|
| `RPMService.java` | Owns `OtlpDataSender`. `sendLogEvents` routes to OTLP. Adds the resource attributes, shutdown, and a `@VisibleForTesting` setter. |
| `transport/DataSenderFactory.java` | Moves HTTP client creation into a public static `createHttpClientWrapper(config, logger, setDefaultJsonContentTypeHeader)` |
| `transport/apache/ApacheHttpClientWrapper.java` | Null check in `logConnectionPoolStatus`, whose debug logging crashed on query-less OTLP URLs. Adds a constructor option to leave out the default `Content-Type: application/json` header; the OTLP client uses it. |
| `transport/ReadResult.java` | `getStatusCode()` and `getResponseBody()` are now public, for the `otlp` subpackage |
| `transport/DataSenderImpl.java` | Comment update: OTLP data usage is now recorded by `OtlpDataSender` |
| `MetricNames.java` | OTLP supportability metric names |
| `newrelic-agent/build.gradle` | `io.opentelemetry.proto:opentelemetry-proto:1.0.0-alpha` is a `shadowIntoJar` (production) dependency, used by the encoders to build the payload and in tests to decode it. Relocated under `com.newrelic.agent.deps.io.opentelemetry.proto`; shares the protobuf-java 3.25.5 already shaded in via `infinite-tracing-protobuf`. |

### Tests

| File | Coverage |
|---|---|
| `transport/otlp/OtlpAttributesTest.java` | Hex conversion: padding, case, invalid and all-zero IDs |
| `transport/otlp/OtlpLogEncoderTest.java` | Every LogRecord field, decoded with the generated OTLP classes; minimal and empty input; invalid IDs; ms vs ns timestamps; severity mapping |
| `transport/otlp/OtlpDataSenderTest.java` | URLs, gzip, Content-Type, per-signal endpoints and headers, license key only for `*.newrelic.com` or `*.nr-data.net`, oversized payload dropped, 200/202 success, 429/503/400/413 retry semantics, metrics |
| `transport/otlp/OtlpTestUtil.java` | Helpers for decoding attributes and IDs |
| `RPMServiceTest.java` (+9 tests), `MockDataSender.java` | Logs routed to OTLP; retryable OTLP log errors rethrown; resource attributes |

## Verification status

**Done**
- `./gradlew -PnoInstrumentation :newrelic-agent:test` for the new tests and the `config`, `logging`, `analytics`, `transport` and `RPMServiceTest` suites: all pass.
- `./gradlew :newrelic-agent:spotbugsMain`: 0 findings.
- New code is within the 160-character line limit from `dev-tools/code-style/java-agent-code-style.xml`.

**Not done yet**
- **No unit tests for the `ApacheHttpClientWrapper` changes.** Neither the null-query check (the debug flag is fixed at JVM startup) nor the option to leave out the default `Content-Type` header is covered.
- **No end-to-end testing:**
  1. Against a local OTel Collector (`otlphttp` receiver + `debug` exporter), to confirm the payloads are well-formed.
  2. Against a New Relic test account, to confirm Log data appears with New Relic attribute names and link to the existing APM entity (logs in context).

## Known limitations and follow-ups

- **Not implemented in v1**:
  - honouring `Retry-After` (the next harvest acts as the backoff)
  - parsing OTLP partial-success responses (New Relic returns an empty body)
  - zstd compression
  - OTLP/gRPC
  - splitting oversized payloads
- **Attribute limits**: New Relic's OTLP limits apply (4095-character values, 255-character keys, 128 attributes per record, 64 per resource). New Relic truncates rather than drops and records an `NrIntegrationError`.
