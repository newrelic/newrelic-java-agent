# OpenTelemetry Exporter OTLP

This instrumentation module works in tandem with the `opentelemetry-sdk-extension-autoconfigure-1.28.0` hybrid agent instrumentation to:
* log the OTLP Metrics response payload when the agent's `audit_mode` config is set to `true`.