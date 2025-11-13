# OpenTelemetry Instrumentation

This instrumentation module weaves parts of the OpenTelemetry SDK to incorporate bits of OpenTelemetry functionality into the New Relic Java agent.

Specifically, it can:

* Detect OpenTelemetry Spans and add them to New Relic Java agent traces as New Relic Spans.
* Detect OpenTelemetry LogRecords and report them as New Relic LogEvents.
* Detect OpenTelemetry dimensional metrics and report them to the APM entity being monitored by the Java agent.
* Autoconfigure the OpenTelemetry SDK so that OpenTelemetry data is sent to New Relic and properly associated with an APM entity guid.

// FIXME update config in doc
## OpenTelemetry Configuration

To use the OpenTelemetry functionality incorporated into the New Relic Java agent you must enable the following config options:

```commandline
-Dotel.java.global-autoconfigure.enabled=true
```

## New Relic Java Agent Configuration

To use the OpenTelemetry Span and dimensional metric functionality incorporated into the New Relic Java agent you must enable the following config options:

Configuration via yaml:

```yaml
  opentelemetry:
    # config to enable different types of telemetry from the OpenTelemetry SDK
    sdk:
      autoconfigure:
        enabled: true
      spans:
        enabled: true
      logs:
        enabled: true
    # instrumentation scope names which are excluded from reporting traces and logs
    instrumentation:
      specific-instrumentation-scope-name-1:
        enabled: true
      specific-instrumentation-scope-name-2:
        enabled: true
    metrics:
      # comma-separated list of meter names which are excluded from reporting metrics
      exclude: foo-module,bar-module
```

Configuration via system property:

```
-Dnewrelic.config.opentelemetry.sdk.autoconfigure.enabled=true
-Dnewrelic.config.opentelemetry.sdk.spans.enabled=true
-Dnewrelic.config.opentelemetry.sdk.logs.enabled=true

# instrumentation scope names which are excluded from reporting traces and logs
-Dnewrelic.config.opentelemetry.instrumentation.[SPECIFIC_INSTRUMENTATION_SCOPE_NAME].enabled=true

# comma-separated list of meter names which are excluded from reporting metrics
-Dnewrelic.config.opentelemetry.metrics.exclude=foo-module,bar-module
```

Configuration via environment variable:

```
NEW_RELIC_OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED=true
NEW_RELIC_OPENTELEMETRY_SDK_SPANS_ENABLED=true
NEW_RELIC_OPENTELEMETRY_SDK_LOGS_ENABLED=true

# instrumentation scope names which are excluded from reporting traces and logs
NEW_RELIC_OPENTELEMETRY_INSTRUMENTATION_[SPECIFIC_INSTRUMENTATION_SCOPE_NAME]_ENABLED=true

# comma-separated list of meter names which are excluded from reporting metrics
NEW_RELIC_OPENTELEMETRY_METRICS_EXCLUDE=foo-module,bar-module
```

## OpenTelemetry Dimensional Metrics

OpenTelemetry APIs can be used to create dimensional metrics which will be detected by the New Relic Java agent and reported to the APM entity being monitored
by the New Relic Java agent.

To use this functionality, enable the feature as documented above, add the required `opentelemetry` dependencies to your application:

```groovy
implementation(platform("io.opentelemetry:opentelemetry-bom:1.44.1"))
implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```

Then utilize the OpenTelemetry APIs to record dimensional metrics:

```java
LongCounter longCounter = GlobalOpenTelemetry.get().getMeterProvider().get("my-application").counterBuilder("my.application.counter").build();
longCounter.

add(1,Attributes.of(AttributeKey.stringKey("foo"), "bar"));
```

Any recorded dimensional metrics can be found in the Metrics Explorer for the associated APM entity and can be used to build custom dashboards.

## OpenTelemetry Spans

Documented below are several approaches for incorporating OpenTelemetry Spans into New Relic Java agent traces.

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

### Spans Emitted From OpenTelemetry Instrumentation

The New Relic Java agent will detect Spans emitted by [OpenTelemetry instrumentation](https://opentelemetry.io/docs/languages/java/instrumentation/). It does
this by weaving the `io.opentelemetry.sdk.trace.SdkTracerProvider` so that it will create a New Relic Tracer each time an OpenTelemetry Span is started and
weaving the `io.opentelemetry.context.Context` to propagate context between New Relic and OpenTelemetry Spans.

Currently, the New Relic Java agent does not load any OpenTelemetry instrumentation it simply detects Spans emitted by OpenTelemetry manual instrumentation,
native instrumentation, library instrumentation, or zero code instrumentation (i.e. bytecode instrumentation that would also require running the OpenTelemetry
Java agent).

Depending on the OpenTelemetry Span `SpanKind`, it may result in the New Relic Java agent starting a transaction (when one doesn't already exist).

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

## OpenTelemetry Logs

OpenTelemetry APIs can be used to create log records which will be detected by the New Relic Java agent and reported to the APM entity being monitored by the
New Relic Java agent. The log records will be reported as log events in New Relic, which will be associated with a transaction if the logging occurred within
one.

To use this functionality, enable the feature as documented above, add the required `opentelemetry` dependencies to your application:

```groovy
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.28.0"))
implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
implementation("io.opentelemetry:opentelemetry-exporter-logging")
```

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