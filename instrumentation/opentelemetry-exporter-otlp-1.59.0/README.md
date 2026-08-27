# OpenTelemetry Exporter OTLP

This instrumentation module works in tandem with the `opentelemetry-sdk-extension-autoconfigure-1.59.0` hybrid agent instrumentation to:
* log the OTLP Metrics response payload when the agent's `audit_mode` config is set to `true`.
* generate the `Supportability/Metrics/Java/OpenTelemetryBridge/export/retry` metric.