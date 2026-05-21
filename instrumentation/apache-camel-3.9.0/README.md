# New Relic Instrumentation: Apache Camel 3.9.0+

Provides distributed tracing and transaction monitoring for applications using [Apache Camel](https://camel.apache.org/) 3.9.0 and above.

## What Is Instrumented

This module hooks into the Apache Camel lifecycle to create and propagate New Relic transactions across Camel routes.

## Configuration

One agent configuration value influences Kafka batch behaviour:

| Config Key | Default | Description                                                                                           |
|---|---|-------------------------------------------------------------------------------------------------------|
| `kafka.spans.distributed_trace.consume_many.enabled` | `false` | When `true`, distributed trace headers are accepted from the first exchange in a batch Kafka consume. |

## Notes

- Transactions will not start for certain routing steps (e.g. `direct:`, `seda:`) and/or logging to prevent noise in telemetry.
