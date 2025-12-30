# OpenTelemetry Instrumentation

This instrumentation module instruments parts of the OpenTelemetry SDK in order to incorporate signals (metrics, logs, and traces) emitted by OpenTelemetry APIs into the New Relic Java agent.

The following functionality is supported:

OpenTelemetry Traces Signals
* Detect when `Span`s are emitted by OpenTelemetry APIs and incorporate them to New Relic Java agent traces.
* Detect [Links between Spans](https://opentelemetry.io/docs/specs/otel/overview/#links-between-spans) and report them to New Relic as `SpanLink` events.
* Detect [Events on Spans](https://opentelemetry.io/docs/concepts/signals/traces/#span-events) and report them to New Relic as `SpanEvent` events.

OpenTelemetry Dimensional Metrics Signals
* Autoconfigure the OpenTelemetry SDK to export dimensional metrics (over OTLP) to the APM entity being monitored by the Java agent.

OpenTelemetry Logs Signals
* Detect when `LogRecord`s are emitted by OpenTelemetry APIs report them to the APM entity being monitored by the Java agent as New Relic LogEvents.

## OpenTelemetry Requirements

The [opentelemetry-sdk-extension-autoconfigure](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-sdk-extension-autoconfigure) dependency (version 1.28.0 or later) must be present in the application being monitored for this instrumentation module to apply. The [opentelemetry-exporter-otlp](https://central.sonatype.com/artifact/io.opentelemetry/opentelemetry-exporter-otlp) dependency (version 1.28.0 or later) must also be present for dimensional metrics to be exported to New Relic.

```groovy
implementation(platform("io.opentelemetry:opentelemetry-bom:1.44.1"))
implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```

Additionally, automatic configuration of the OpenTelemetry SDK must be enabled by one of the following options:

System property:
```commandline
-Dotel.java.global-autoconfigure.enabled=true
```

Environment variable:
```commandline
export OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED=true
```

Programmatic:
```java
/*
 * The opentelemetry-sdk-extension-autoconfigure dependency needs to be initialized
 * by either setting -Dotel.java.global-autoconfigure.enabled=true or calling the
 * following API in order for the New Relic Java agent instrumentation to load.
 */
private static final OpenTelemetry OPEN_TELEMETRY_SDK = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
```

## New Relic Java Agent Configuration

Telemetry signals (Logs, Metrics, and Traces) emitted by OpenTelemetry APIs can be incorporated into the Java agent and controlled by the following config options.

Configuration via YAML:

```yaml
  # Telemetry signals (Logs, Metrics, and Traces) emitted by OpenTelemetry APIs can
  # be incorporated into the Java agent and controlled by the following config options.
  opentelemetry:

    # Set to true to allow individual OpenTelemetry signals to be enabled, false to disable all OpenTelemetry signals.
    # Default is false.
    enabled: true

    # OpenTelemetry Logs signals.
    logs:

      # Set to true to enable OpenTelemetry Logs signals.
      # Default is false.
      enabled: true

    # OpenTelemetry Metrics signals.
    metrics:

      # Set to true to enable OpenTelemetry Metrics signals.
      # Default is false.
      enabled: true

      # A comma-delimited string of OpenTelemetry Meters (e.g. "MeterName1,MeterName2") whose signals should be included. 
      # By default, all Meters are included. This will override any default Meter excludes in the agent, effectively re-enabling them.
      include: "MeterName1,MeterName2" 

      # A comma-delimited string of OpenTelemetry Meters (e.g. "MeterName3,MeterName4") whose signals should be excluded. 
      # This takes precedence over all other includes/excludes sources, effectively disabling the listed Meters.
      exclude: "MeterName3,MeterName4" 

    # OpenTelemetry Traces signals.
    traces:

      # Set to true to enable OpenTelemetry Traces signals.
      # Default is false.
      enabled: true

      # A comma-delimited string of OpenTelemetry Tracers (e.g. "TracerName1,TracerName2") whose signals should be included. 
      # By default, all Tracers are included. This will override any default Tracer excludes in the agent, effectively re-enabling them.
      include: "TracerName1,TracerName2"

      # A comma-delimited string of OpenTelemetry Tracers (e.g. "TracerName3,TracerName4") whose signals should be excluded. 
      # This takes precedence over all other includes/excludes sources, effectively disabling the listed Tracers.
      exclude: "TracerName3,TracerName4"
```

Configuration via system property:

```
-Dnewrelic.config.opentelemetry.enabled=true

-Dnewrelic.config.opentelemetry.logs.enabled=true

-Dnewrelic.config.opentelemetry.metrics.enabled=true
-Dnewrelic.config.opentelemetry.metrics.include=MeterName1,MeterName2
-Dnewrelic.config.opentelemetry.metrics.exclude=MeterName3,MeterName4

-Dnewrelic.config.opentelemetry.traces.enabled=true
-Dnewrelic.config.opentelemetry.traces.include=TracerName1,TracerName2
-Dnewrelic.config.opentelemetry.traces.exclude=TracerName3,TracerName4
```

Configuration via environment variable:

```
NEW_RELIC_OPENTELEMETRY_ENABLED=true

NEW_RELIC_OPENTELEMETRY_LOGS_ENABLED=true

NEW_RELIC_OPENTELEMETRY_METRICS_ENABLED=true
NEW_RELIC_OPENTELEMETRY_METRICS_INCLUDE=MeterName1,MeterName2
NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE=MeterName3,MeterName4

NEW_RELIC_OPENTELEMETRY_TRACES_ENABLED=true
NEW_RELIC_OPENTELEMETRY_TRACES_INCLUDE=TracerName1,TracerName2
NEW_RELIC_OPENTELEMETRY_TRACES_EXCLUDE=TracerName3,TracerName4
```

### Deprecated Config

The following deprecated config option should no longer be used and is only kept for backwards compatibility.

This config was originally used to enable/disable metrics signals and has been replaced by `opentelemetry.metrics.enabled`:

```yaml
  opentelemetry:
    sdk:
      autoconfigure:
        enabled: true
```

## OpenTelemetry Dimensional Metrics Signals

The [OpenTelemetry Metrics API](https://opentelemetry.io/docs/specs/otel/metrics/api/) can be used to create dimensional metrics which will be exported by the OpenTelemetry SDK to New Relic over OTLP. The dimensional metrics will be decorated with the `entity.guid` of the APM entity being monitored by the New Relic Java agent.

Example of OpenTelemetry APIs being used to record dimensional metrics:

```java
    // Generate LongCounter dimensional metrics
    LongCounter longCounter = GlobalOpenTelemetry.get()
      .getMeterProvider()
      .get("opentelemetry-metrics-api-demo")
      .counterBuilder("opentelemetry-metrics-api-demo.longcounter")
      .build();
    longCounter.add(1, Attributes.of(AttributeKey.stringKey("LongCounter"), "foo"));
    
    // Generate DoubleHistogram dimensional metrics
    DoubleHistogram doubleHistogram = GlobalOpenTelemetry.get()
      .getMeterProvider()
      .get("opentelemetry-metrics-api-demo")
      .histogramBuilder("opentelemetry-metrics-api-demo.histogram")
      .build();
    doubleHistogram.record(3, Attributes.of(AttributeKey.stringKey("DoubleHistogram"), "foo"));
    
    // Generate DoubleGauge dimensional metrics
    DoubleGauge doubleGauge = GlobalOpenTelemetry.get()
      .getMeterProvider()
      .get("opentelemetry-metrics-api-demo")
      .gaugeBuilder("opentelemetry-metrics-api-demo.gauge")
      .build();
    doubleGauge.set(5, Attributes.of(AttributeKey.stringKey("DoubleGauge"), "foo"));

    // Generate LongUpDownCounter dimensional metrics
    LongUpDownCounter longUpDownCounter = GlobalOpenTelemetry.get()
      .getMeterProvider()
      .get("opentelemetry-metrics-api-demo")
      .upDownCounterBuilder("opentelemetry-metrics-api-demo.updowncounter")
      .build();
    longUpDownCounter.add(7, Attributes.of(AttributeKey.stringKey("LongUpDownCounter"), "foo"));
```

Any recorded dimensional metrics can be found in the Metrics Explorer for the associated APM entity and can be used to build custom dashboards.

## OpenTelemetry Traces Signals

Documented below are several approaches for incorporating OpenTelemetry Traces (aka Spans) into New Relic Java agent traces.

### `@WithSpan` Annotation

The New Relic Java agent will detect usage of the OpenTelemetry [@WithSpan](https://opentelemetry.io/docs/zero-code/java/agent/annotations/) annotation. The
`@WithSpan` annotation can be used as an alternative to the `@Trace` annotation.

This does not currently support the following config options:

* [Suppressing @WithSpan instrumentation](https://opentelemetry.io/docs/zero-code/java/agent/annotations/#suppressing-withspan-instrumentation)
* [Creating spans around methods with otel.instrumentation.methods.include](https://opentelemetry.io/docs/zero-code/java/agent/annotations/#creating-spans-around-methods-with-otelinstrumentationmethodsinclude)

Note that OpenTelemetry config properties can be set through environment or system properties, like our agent, and eventually through a config file. We can use
our existing OpenTelemetry instrumentation model to get access to the normalized version of the instrumentation settings to include and exclude methods and pass
those to the core agent through the bridge.

See `ClassTransformerConfigImpl.java` for implementation details of the `@WithSpan` annotation.

### Spans Emitted From OpenTelemetry Tracing API

The New Relic Java agent will detect Spans emitted by the [OpenTelemetry Tracing API](https://opentelemetry.io/docs/specs/otel/trace/api/) for most manual, library, and native [instrumentation types](https://opentelemetry.io/docs/languages/java/instrumentation/#instrumentation-categories) and incorporate them into New Relic traces. 

It does this by weaving the `io.opentelemetry.sdk.trace.SdkTracerProvider` so that it will create a New Relic Tracer each time an OpenTelemetry Span is started and
weaving the `io.opentelemetry.context.Context` to propagate context between New Relic and OpenTelemetry Spans.

#### Translating OpenTelemetry SpanKinds To The New Relic Span Data Model

Depending on the OpenTelemetry Span `SpanKind`, it may result in the New Relic Java agent starting a transaction (when one doesn't already exist). Also see [attribute-mappings.json](src/main/resources/attribute-mappings.json) for the complete attribute mapping rules.

* `SpanKind.INTERNAL`
    * Creating a span with no `SpanKind`, which defaults to `SpanKind.INTERNAL`, will not start a transaction
    * If `SpanKind.INTERNAL` spans occur within an already existing New Relic transaction they will be included in the trace
* `SpanKind.CLIENT`
    * Creating a span with `SpanKind.CLIENT` will not start a transaction. If a `CLIENT` span has certain db attributes it will be treated as a DB span, and
      other specific attributes will cause it to be treated as an external span
    * If `SpanKind.CLIENT` spans occur within an already existing New Relic transaction they will be included in the trace
* `SpanKind.SERVER`
    * Creating a span with `SpanKind.SERVER` will start a `WebTransaction/Uri/*` transaction.
    * If `SpanKind.SERVER` spans occur within an already existing New Relic transaction they will be included in the trace
* `SpanKind.CONSUMER`
    * Creating a span with `SpanKind.CONSUMER` will start a `OtherTransaction/*` transaction.
    * If `SpanKind.CONSUMER` spans occur within an already existing New Relic transaction they will be included in the trace
* `SpanKind.PRODUCER`
    * Creating a span with `SpanKind.PRODUCER` will not start a transaction. There is no explicit processing for `PRODUCER` spans currently.
    * If `SpanKind.PRODUCER` spans occur within an already existing New Relic transaction they will be included in the trace (though it's effectively no
      different from a `SpanKind.INTERNAL` span)

### Span Links From OpenTelemetry Tracing API

When processing OpenTelemetry Spans, any [Links](https://opentelemetry.io/docs/specs/otel/trace/api/#link) associated with Spans will be captured and reported to New Relic as `SpanLink` events, which enhance the distributed tracing experience by providing backwards and forwards links between Spans from different traces.

`SpanLink` events are included in the `span_event_data` collector payload and do not have their own event sampling reservoir.

Example of adding `Link`s via the OpenTelemetry Traces APIs:

```java
    public void createSpanLinks() {
        System.out.println("Called doSpanLinks");
        try {
            SpanContext spanContext = upstreamSpan();
            for (int i = 0; i < 20; i++) {
                downstreamSpan(spanContext, i);
            }
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
    }

    @Trace(dispatcher = true)
    public SpanContext upstreamSpan() throws InterruptedException {
        Span span = tracer.spanBuilder("upstreamSpan").startSpan();
        SpanContext spanContext;
        try (Scope scope = span.makeCurrent()) {
            System.out.println("Called upstreamSpan");
            spanContext = span.getSpanContext();
            Thread.sleep(1000);
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
        return spanContext;
    }

    @Trace(dispatcher = true)
    public void downstreamSpan(SpanContext spanContext, int iteration) throws InterruptedException {
        Span span = tracer.spanBuilder("downstreamSpan").addLink(spanContext, Attributes.builder().put("iteration", iteration).put("customLinkAttribute", "someValue").build()).startSpan();
        try (Scope scope = span.makeCurrent()) {
            System.out.println("Called downstreamSpan");
            Thread.sleep(1000);
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }
```

### Span Events From OpenTelemetry Tracing API

When processing OpenTelemetry Spans, any [Events](https://opentelemetry.io/docs/specs/otel/trace/api/#add-events) associated with Spans will be captured and reported to New Relic as `SpanEvent` events, which enhance the distributed tracing experience by providing log-like records that add extra details about what happened within a Span's execution.

`SpanEvent` events are included in the `span_event_data` collector payload and do not have their own event sampling reservoir.

Example of adding `Event`s via the OpenTelemetry Traces APIs. Note that `recordException` is a specialized variant of `addEvent` for recording exception events:

```java
    @Trace(dispatcher = true)
    public void createSpanEvents() {
        System.out.println("Called createSpanEvents");
        Span span = tracer.spanBuilder("spanWithEvents").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span
                    .addEvent("event1")
                    .addEvent("event2", Instant.ofEpochSecond(System.nanoTime()))
                    .addEvent("event3", Attributes.builder().put("foo", "bar").build())
                    .addEvent("event4", Attributes.builder().put("bar", "baz").build(), System.nanoTime(), TimeUnit.NANOSECONDS)
                    .addEvent("event5", Attributes.builder().put("baz", "buz").build(), Instant.ofEpochSecond(System.nanoTime()))
                    .addEvent("event6", System.nanoTime(), TimeUnit.NANOSECONDS);
            Thread.sleep(1000);
            throw new RuntimeException("Exception in createSpanEventException");
        } catch (Throwable t) {
            span.recordException(t);
            span.recordException(t, Attributes.builder()
                    .put("exception.message", t.getMessage())
                    .put("exception.type", t.getClass().getName())
                    .put("exception.stacktrace", Arrays.toString(t.getStackTrace()))
                    .build());
        } finally {
            span.end();
        }
    }
```

## OpenTelemetry Logs Signals

The New Relic Java agent will detect LogRecords emitted by the [OpenTelemetry Logs API](https://opentelemetry.io/docs/specs/otel/logs/api/) and incorporate them into New Relic log events associated with the APM entity being monitored. The logs will be associated with a New Relic transaction if the logging occurred within one. 

Note: It is recommended that OpenTelemetry instrumentation for a particular logging framework (e.g. Logback, Log4j) should not be used alongside New Relic Java agent instrumentation for the same logging framework. Doing so could result in duplicate log events being reported to New Relic.

Example usage of OpenTelemetry Logs APIs:

```java
    // create LogRecordExporter
private static final SystemOutLogRecordExporter systemOutLogRecordExporter = SystemOutLogRecordExporter.create();

// create LogRecordProcessor
private static final LogRecordProcessor logRecordProcessor = SimpleLogRecordProcessor.create(systemOutLogRecordExporter);

// create Attributes
private static final Attributes attributes = Attributes.builder()
        .put("service.name", NewRelic.getAgent().getConfig().getValue("app_name", "unknown"))
        .put("service.version", "4.5.1")
        .put("environment", "production")
        .build();

// create Resource
private static final Resource customResource = Resource.create(attributes);

// create SdkLoggerProvider
private static final SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(logRecordProcessor)
        .setResource(customResource)
        .build();

// create LoggerBuilder
private static final LoggerBuilder loggerBuilder = sdkLoggerProvider
        .loggerBuilder("demo-otel-logger")
        .setInstrumentationVersion("1.0.0")
        .setSchemaUrl("https://opentelemetry.io/schemas/1.0.0");

// create Logger
private static final Logger logger = loggerBuilder.build();

// utility method to build and emit OpenTelemetry log records
public static void emitOTelLogs(Severity severity) {
    // create LogRecordBuilder
    LogRecordBuilder logRecordBuilder = logger.logRecordBuilder();

    Instant now = Instant.now();
    logRecordBuilder
//                .setContext()
            .setBody("Generating OpenTelemetry LogRecord")
            .setSeverity(severity)
            .setSeverityText("This is the severity text")
            .setAttribute(AttributeKey.stringKey("foo"), "bar")
            .setObservedTimestamp(now)
            .setObservedTimestamp(now.toEpochMilli(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .setTimestamp(now)
            .setTimestamp(now.toEpochMilli(), java.util.concurrent.TimeUnit.MILLISECONDS);

    if (severity == Severity.ERROR) {
        try {
            throw new RuntimeException("This is a test exception for severity ERROR");
        } catch (RuntimeException e) {
            logRecordBuilder.setAttribute(AttributeKey.stringKey("exception.message"), e.getMessage());
            logRecordBuilder.setAttribute(AttributeKey.stringKey("exception.type"), e.getClass().getName());
            logRecordBuilder.setAttribute(AttributeKey.stringKey("exception.stacktrace"), Arrays.toString(e.getStackTrace()));
        }
    }

    logRecordBuilder.emit();
}
```