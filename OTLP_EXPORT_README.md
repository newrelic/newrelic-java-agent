# OTLP Export for Log Events

The Java agent can export `LogEvent` data over **OTLP/HTTP with binary protobuf encoding**. This works with New Relic's OTLP endpoint.

- **Logs**: when enabled, sent via OTLP *instead of* the collector (`log_event_data`).
- **Infinite Tracing**: no interaction. With Infinite Tracing on, spans never reach the span reservoir, so neither the collector nor OTLP receives them.
- **Attribute names**: New Relic names are kept, not translated to OTel semantic conventions, so the New Relic UI keeps working.
- **Default**: off. Export is opt-in.

## Why this approach

Three options were evaluated:

1. A hand-written OTLP protobuf encoder.
2. Reusing the OpenTelemetry Java SDK exporters.
3. OTLP/JSON on the agent's existing JSON library.

The SDK exporters were rejected for these reasons:
- **Size**: ~1.4 MB of OTel jars, or ~4.4 MB with okhttp/okio/kotlin.
- **Copies**: every harvested event would need converting to `LogRecordData`.
- **Unstable APIs**: the marshalers and senders are in `*.internal` packages.
- **Shading**: ServiceLoader lookups, a multi-release jar, `GlobalOpenTelemetry` self-metrics, and a second HTTP stack with its own proxy/TLS setup.

The hand-written encoder:
- **Dependencies**: adds no runtime dependencies.
- **Transport**: reuses the agent's existing Apache HTTP client, so the agent's proxy, `ca_bundle_path` and timeout settings apply.
- **Spec stability**: the OTLP log protos it covers are marked Stable.

## Configuration

```yaml
common: &default_settings
  otlp_export:
    enabled: false        # opt-in
    endpoint:             # base endpoint; default is derived from the license key region
    headers:              # "k1=v1,k2=v2", used by every signal without its own headers
    logs:
      enabled: true       # send logs via OTLP instead of the collector
      endpoint:           # full URL, used as-is (e.g. https://collector.newrelic.com/v1/logs)
      headers:            # replaces otlp_export.headers for logs
```

Every setting can also be set through an environment variable or a system property. For example:
- `NEW_RELIC_OTLP_EXPORT_ENABLED=true`
- `NEW_RELIC_OTLP_EXPORT_LOGS_ENDPOINT=...`
- `-Dnewrelic.config.otlp_export.logs.headers=...`

### Endpoint resolution

These rules follow the OTel SDK convention (`OTEL_EXPORTER_OTLP_ENDPOINT` vs `OTEL_EXPORTER_OTLP_<SIGNAL>_ENDPOINT`):

- A per-signal `endpoint` is used **as-is**.
- Otherwise, `/v1/logs` is appended to the base `endpoint`. Trailing slashes on the base are removed first.
- If the base `endpoint` isn't set, it's derived from the license key region, using the same logic as the collector host:
  - `https://collector.newrelic.com` when the license key has no region prefix.
  - `https://collector.<region>.nr-data.net` otherwise, e.g. `collector.eu01.nr-data.net`.
- [FedRAMP](https://docs.newrelic.com/docs/security/security-privacy/compliance/fedramp-compliant-endpoints/) (`https://gov-collector.newrelic.com`) doesn't follow that host pattern, so it must be configured explicitly. 

### Headers and authentication

- **Per-signal headers replace the shared headers** for that signal; they aren't merged. This matches how the OTel SDK treats `OTEL_EXPORTER_OTLP_LOGS_HEADERS`. Credentials meant for one endpoint are therefore never sent to another.
- **The license key is sent only to New Relic endpoints.** The `api-key: <license key>` header is added automatically only when the endpoint host ends in `newrelic.com` or `.nr-data.net`, and only if no `api-key` header is configured. For any other host it's never sent, and the agent logs an INFO message.
- **YAML caveat**: in YAML, an empty `headers:` line counts as "not set", so the signal falls back to the shared headers. Use `headers: ""` to send none.

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

`{signal}` is `v1/logs`.

In audit mode, each OTLP request is logged with its URL, event count, compressed size and response code. The protobuf body itself isn't logged.

## Files changed

### New: `newrelic-agent/src/main/java/com/newrelic/agent/transport/otlp/`

| File | Purpose |
|---|---|
| `ProtobufWriter.java` | Minimal proto3 wire encoder: varint, fixed32/64, double, string, bytes, and nested messages. Each nested message's length prefix is written in place, with no second pass. |
| `OtlpAttributes.java` | Shared encoding for `KeyValue`/`AnyValue`, `Resource` and `InstrumentationScope`, plus hex ID → bytes conversion |
| `OtlpLogEncoder.java` | Builds `ExportLogsServiceRequest` from `LogEvent`s |
| `OtlpDataSender.java` | Handles gzip, the size check, per-signal URLs and headers, the license key host check, the HTTP POST, status handling and metrics |

### New: config

| File | Purpose |
|---|---|
| `config/OtlpExportConfig.java` | Interface: enabled flags, per-signal endpoints, per-signal headers |
| `config/OtlpExportConfigImpl.java` | Parses the `otlp_export` stanza, resolves endpoints from the region, parses headers, disables export in serverless mode |

### Modified

| File | Change |
|---|---|
| `RPMService.java` | Owns an optional `OtlpDataSender`. `sendLogEvents` routes to OTLP when OTLP logs are enabled. Adds the resource attributes, shutdown, and a `@VisibleForTesting` setter. |
| `config/AgentConfig.java`, `config/AgentConfigImpl.java` | Register `getOtlpExportConfig()`, passing the license key region and serverless flag |
| `transport/DataSenderFactory.java` | Moves HTTP client creation into a public static `createHttpClientWrapper(config, logger[, setDefaultJsonContentTypeHeader])` |
| `transport/apache/ApacheHttpClientWrapper.java` | Null check in `logConnectionPoolStatus`, whose debug logging crashed on query-less OTLP URLs. Adds a constructor option to leave out the default `Content-Type: application/json` header; the OTLP client uses it. |
| `transport/ReadResult.java` | `getStatusCode()` and `getResponseBody()` are now public, for the `otlp` subpackage |
| `transport/DataSenderImpl.java` | Comment update: OTLP data usage is now recorded by `OtlpDataSender` |
| `MetricNames.java` | OTLP supportability metric names |
| `newrelic-agent/src/main/resources/newrelic.yml` | Documents the `otlp_export` stanza |
| `.fleetControl/schemaGeneration/reference-newrelic.yml`, `.fleetControl/schemas/config.json` | Documents the stanza and regenerates the Fleet Control schema (additive change) |
| `newrelic-agent/build.gradle` | Test-only dependency `io.opentelemetry.proto:opentelemetry-proto:1.0.0-alpha`, used to decode encoder output in tests. Compatible with the agent's protobuf-java 3.25.5. |

### Tests

| File | Coverage |
|---|---|
| `transport/otlp/ProtobufWriterTest.java` | Varint edge cases, fixed-width types, nested length prefixes (including multi-byte), deep nesting, misuse errors |
| `transport/otlp/OtlpAttributesTest.java` | Hex conversion: padding, case, invalid and all-zero IDs |
| `transport/otlp/OtlpLogEncoderTest.java` | Every LogRecord field, decoded with the generated OTLP classes; minimal and empty input; invalid IDs; ms vs ns timestamps; severity mapping |
| `transport/otlp/OtlpDataSenderTest.java` | URLs, gzip, Content-Type, per-signal endpoints and headers, license key only for `*.newrelic.com` or `*.nr-data.net`, oversized payload dropped, 200/202 success, 429/503/400/413 retry semantics, metrics |
| `transport/otlp/OtlpTestUtil.java` | Helpers for decoding attributes and IDs |
| `config/OtlpExportConfigImplTest.java` | Defaults, signal enablement, serverless, region endpoint, endpoint override and fallback, header parsing and replacement, env var and system property overrides, `AgentConfigImpl` wiring |
| `RPMServiceTest.java` (+9 tests), `MockDataSender.java` | Logs routed to OTLP only when enabled; retryable OTLP log errors rethrown; resource attributes |

## Verification status

**Done**
- `./gradlew -PnoInstrumentation :newrelic-agent:test` for the new tests and the `config`, `logging`, `analytics`, `transport` and `RPMServiceTest` suites: all pass.
- `./gradlew :newrelic-agent:spotbugsMain`: 0 findings.
- New code is within the 160-character line limit from `dev-tools/code-style/java-agent-code-style.xml`. This build has no `googleJavaFormat` task.

**Not done yet**
- **No unit tests for the `ApacheHttpClientWrapper` changes.** Neither the null-query check (the debug flag is fixed at JVM startup) nor the option to leave out the default `Content-Type` header is covered.
- **No end-to-end testing:**
  1. Against a local OTel Collector (`otlphttp` receiver + `debug` exporter), to confirm the payloads are well formed.
  2. Against a New Relic test account, to confirm Log data appears with New Relic attribute names and link to the existing APM entity (logs in context).

## Known limitations and follow-ups

- **Entity linking**: whether New Relic links OTLP data to the existing APM entity, rather than creating a new OTel service entity, still needs end-to-end confirmation.
- **Not implemented in v1**:
  - honouring `Retry-After` (the next harvest acts as the backoff)
  - parsing OTLP partial-success responses (New Relic returns an empty body)
  - zstd compression
  - OTLP/gRPC
  - splitting oversized payloads
- **Attribute limits**: New Relic's OTLP limits apply (4095-character values, 255-character keys, 128 attributes per record, 64 per resource). New Relic truncates rather than drops and records an `NrIntegrationError`.
