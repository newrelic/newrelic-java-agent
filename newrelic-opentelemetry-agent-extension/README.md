# NewRelic OpenTelemetry Agent Extension

An [OpenTelemetry Java Agent extension](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md) which
implements the [Java agent API](https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/guide-using-java-agent-api/) using the OpenTelemetry API.

## Usage

To use, follow
the [OpenTelemetry Java Agent extension](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md)
instructions, running your application with the OpenTelemetry java agent and specifying the path to the NewRelic OpenTelemetry Agent extension:

```shell
java \
 -javaagent:path/to/opentelemetry-javaagent.jar \
 -Dotel.javaagent.extensions=path/to/newrelic-opentelemetry-agent-extension.jar \
 -jar app.jar
```

The process for downloading the OpenTelemetry Java Agent and the NewRelic OpenTelemetry Agent extension will vary from project to project. However, all projects
will need to download the New Relic OpenTelemetry Agent extension, which is published to maven coordinates:

```xml

<dependency>
    <groupId>com.newrelic.agent.java</groupId>
    <artifactId>newrelic-opentelemetry-agent-extension</artifactId>
    <version>{{PROJECT_VERSION}}-alpha</version>
</dependency>
```

**IMPORTANT**: This package is marked "-alpha". All APIs and behaviors are subject to change. Please use with caution and be sure to check the release notes for
changes before upgrading.

See [OpenTelemetry Java Getting started guide](https://docs.newrelic.com/docs/more-integrations/open-source-telemetry-integrations/opentelemetry/get-started/opentelemetry-tutorial-java/)
for information on configuring the OpenTelemetry Java agent to export to New Relic.

## Supported Operations

Calls to the [Java agent API](https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/guide-using-java-agent-api/) will be routed through the
OpenTelemetry API. Note that many concepts of the New Relic API do not map to an equivalent in the OpenTelemetry API. When an API is called which is not bridged
to OpenTelemetry, the extension will log details from logger named `com.newrelic.opentelemetry.OpenTelemetryNewRelic` at `FINER` level (if `FINEST` level is
enabled, a stacktrace to the calling code is included).

The following operations are supported:

* NewRelic Metric APIs for recording custom timeslice metrics. Record to OpenTelemetry dimensional metrics, where the name of the metric is `newrelic.timeslice.value` for `recordMetric`, `recordResponseTimeMetric`, and `newrelic.timeslice.counter.value` for `incrementCounter`. The name of timeslice metric is set to the value of a dimension on metric with key `metricTimesliceName`.
  * `NewRelic.recordMetric(String, float)`
  * `NewRelic.recordResponseTimeMetric(String, long)`
  * `NewRelic.incrementCounter(String)`
  * `NewRelic.incrementCounter(String, int)`
  * `MetricAggregator.recordMetric(String, float)`
  * `MetricAggregator.recordResponseTimeMetric(String, long)`
  * `MetricAggregator.incrementCounter(String)`
  * `MetricAggregator.incrementCounter(String, int)`
* NewRelic Error APIs for recording errors. Record the error on whatever OpenTelemetry span is currently active (i.e. `Span.current()`) using `recordException`. Sets the active span status to `ERROR`.
  * `NewRelic.noticeError(Throwable, Map<String, ?>)`
  * `NewRelic.noticeError(Throwable)`
  * `NewRelic.noticeError(String, Map<String, ?>)`
  * `NewRelic.noticeError(String)`
  * `NewRelic.noticeError(Throwable, Map<String, ?>, boolean)`
  * `NewRelic.noticeError(Throwable, boolean)`
  * `NewRelic.noticeError(String, Map<String, ?>, boolean)`
  * `NewRelic.noticeError(String, boolean)`
  * `ErrorApi.noticeError(Throwable, Map<String, ?>)`
  * `ErrorApi.noticeError(Throwable)`
  * `ErrorApi.noticeError(String, Map<String, ?>)`
  * `ErrorApi.noticeError(String)`
  * `ErrorApi.noticeError(Throwable, Map<String, ?>, boolean)`
  * `ErrorApi.noticeError(Throwable, boolean)`
  * `ErrorApi.noticeError(String, Map<String, ?>, boolean)`
  * `ErrorApi.noticeError(String, boolean)`
* NewRelic TracedMethod APIs for adding custom attributes. Record the custom attributes to whatever OpenTelemetry span is currently active (i.e. `Span.current()`).
  * `TracedMethod.addCustomAttribute(String, Number)`
  * `TracedMethod.addCustomAttribute(String, Strg)`
  * `TracedMethod.addCustomAttribute(String, boolean)`
  * `TracedMethod.addCustomAttributes(Map<String, Object>)`
* NewRelic Insights API for recording custom events. Record the event as an OpenTelemetry LogRecord following the [semantic conventions for events](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/events.md). The `event.domain` is set to `newrelic.agent_api`, the `event.name` is set to the name of the custom event.
  * `Insights.recordCustomEvent(String, Map<String, ?>)`