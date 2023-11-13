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

Calls to the [Java agent API](https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/guide-using-java-agent-api/) API will be routed through the
OpenTelemetry API. Note that many concepts of the New Relic API do not map to an equivalent in the OpenTelemetry API. When an API is called which is not bridged
to OpenTelemetry, the extension will log details from logger named `com.newrelic.opentelemetry.OpenTelemetryNewRelic` at `FINER` level (if `FINEST` level is
enabled, a stacktrace to the calling code is included).

TODO: add table defining which NewRelic APIs are supported and describe the behavior of each

See [OpenTelemetry Java Getting started guide](https://docs.newrelic.com/docs/more-integrations/open-source-telemetry-integrations/opentelemetry/get-started/opentelemetry-tutorial-java/)
for information on configuring the OpenTelemetry Java agent to export to New Relic.
