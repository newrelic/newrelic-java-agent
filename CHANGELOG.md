# Changelog
Noteworthy changes to the agent are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
* TBD

## Version 6.5.0 (2021-4-26)

### New Features and Improvements:
* The agent no longer bundles SSL certificates with it and the `use_private_ssl` option that configured the agent to use
  the previously bundled certificates has been removed. By default, the agent will use the SSL truststore provided by 
  the JVM unless it is explicitly configured to use a different truststore with the ca_bundle_path option. See
  [Configuring your SSL certificates](https://docs.newrelic.com/docs/agents/java-agent/configuration/configuring-your-ssl-certificates/)
  for more details.
  ([#245](https://github.com/newrelic/newrelic-java-agent/pull/245))

### Fixes:
* Fixes an issue that could cause incorrect transaction naming when using JAX-RS sub-resources.
  ([#234](https://github.com/newrelic/newrelic-java-agent/pull/234))
* Reactor Netty instrumentation improvements and fixes.
  ([#237](https://github.com/newrelic/newrelic-java-agent/pull/237),
  [#239](https://github.com/newrelic/newrelic-java-agent/pull/239),
  [#243](https://github.com/newrelic/newrelic-java-agent/pull/243))

### Deprecation Notice

* Java 7 compatibility deprecation

In order to continue to innovate and efficiently provide new capabilities to our customers who run on the JVM, Java 7 
support has been deprecated and this will be the last version of the agent compatible with it.

If you are running Java 7, you may continue to use Java agent 6.5.0 or lower.

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 6.4.0 (2021-1-27)

### New Features and Improvements:
* Spring Webflux/Netty Reactor instrumentation improvements for enhanced tracing across asynchronous thread hops
([#174](https://github.com/newrelic/newrelic-java-agent/pull/174), [#190](https://github.com/newrelic/newrelic-java-agent/pull/190),
[#195](https://github.com/newrelic/newrelic-java-agent/pull/195)).
* Infinite tracing will now utilize a backoff sequence on retries. ([#180](https://github.com/newrelic/newrelic-java-agent/pull/180))
* New distributed tracing APIs have been added to better support general use cases for
propagating distributed tracing headers. In particular the new APIs provide enhanced support for [W3C Trace Context](https://www.w3.org/TR/trace-context/) but
are flexible enough to support other header protocols. Previous distributed tracing APIs have been deprecated and are subject to removal in a
future agent release. See [documentation here](https://docs.newrelic.com/docs/agents/java-agent/api-guides/guide-using-java-agent-api#trace-calls).
([#188](https://github.com/newrelic/newrelic-java-agent/pull/188))
  * [`Transaction.insertDistributedTraceHeaders(Headers)`](https://newrelic.github.io/java-agent-api/javadoc/com/newrelic/api/agent/Transaction.html#insertDistributedTraceHeaders(com.newrelic.api.agent.Headers))
is used to create and insert distributed tracing headers (both newrelic and W3C Trace Context) into a `Headers` data structure.
  * [`Transaction.acceptDistributedTraceHeaders(TransportType, Headers)`](https://newrelic.github.io/java-agent-api/javadoc/com/newrelic/api/agent/Transaction.html#acceptDistributedTraceHeaders(com.newrelic.api.agent.TransportType,com.newrelic.api.agent.Headers))
  is used to accept the distributed tracing headers sent from the calling service and link these services together in a distributed trace.

### Fixes:
* Updated the Java agent’s snakeyaml dependency to 1.27. ([#182](https://github.com/newrelic/newrelic-java-agent/pull/182))
* In some environments the jar collector service could lead to high CPU utilization at application startup.
The agent now provides a configurable rate limiter, with a reasonable default, for processing jars detected in the application’s environment.
See [documentation here](https://docs.newrelic.com/docs/agents/java-agent/configuration/java-agent-configuration-config-file#jar-collector).
([#183](https://github.com/newrelic/newrelic-java-agent/pull/183))

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 6.3.0 (2020-12-17)

### New Features and Improvements:
* [Adds support for Spring Webflux 5.3.+](https://github.com/newrelic/newrelic-java-agent/pull/121).
* [Adds configuration for ignoring netty reactor errors](https://github.com/newrelic/newrelic-java-agent/pull/130). The relevant configuration property is `reactor-netty.errors.enabled`. Error reporting is enabled by default.
* Adds support for [Scala 2.13](https://github.com/newrelic/newrelic-java-agent/pull/109), including [Akka Http](https://github.com/newrelic/newrelic-java-agent/pull/149) and [Play](https://github.com/newrelic/newrelic-java-agent/pull/94). Thank you [junder31](https://github.com/junder31) for these contributions.

### Fixes:
* The netty-4.0.8 instrumentation would [sometimes not start a Transaction on `channelRead`](https://github.com/newrelic/newrelic-java-agent/pull/148), potentially affecting instrumentation dependent on it including: Spring, Akka and Play.
* Updates the Java agent’s [Apache HttpClient dependency to 5.13](https://github.com/newrelic/newrelic-java-agent/pull/145).
* Spring Webclient could report the [wrong URL when multiple HTTP calls to several URLs occurred in parallel](https://github.com/newrelic/newrelic-java-agent/pull/129). Thank you [veklov](https://github.com/veklov) for contributing this fix.

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 6.2.1 (2020-11-17)
[Fixes an issue where Spring-Webflux applications with endpoints returning no or empty content could become unresponsive](https://github.com/newrelic/newrelic-java-agent/pull/115)

## Version 6.2.0 (2020-11-10)

### New Features and Improvements:
* **Support for Real Time Profiling of your JVMs.** The agent can now run in parallel with the JFR Daemon to provide Realtime Profiling of your JVMs using Java Flight Recorder! Read more about it in our [blog](https://blog.newrelic.com/product-news/real-time-java-profiling/) and  [documentation](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-profiling-java-using-jfr-metrics).
* [The agent now supports parsing cgroup ids that do not contain `docker-`](https://github.com/newrelic/newrelic-java-agent/pull/87). Previously, the agent only supported docker cgroup ids that contained `docker-` in them, such as `1:cpu:/system.slice/docker-12345abcde.scope`. The agent now supports parsing cgroup ids such as `1:cpu:/system.slice/crio-12345abcde.scope`.
* [Adds support for Akka Http Core 10.2.0](https://github.com/newrelic/newrelic-java-agent/pull/90). Thank you [jobicarter](https://github.com/jobicarter) for reporting this issue.
* [Adds support for gRPC 1.30.0](https://github.com/newrelic/newrelic-java-agent/pull/92). Thank you [jef](https://github.com/jef) for submitting this request and trying it out.
* [Adds support for `map` and `flatmap` async external calls in spring webflux framework](https://github.com/newrelic/newrelic-java-agent/pull/93).  Previously the agent didn’t support client calls that occurred within the `map` or `flatmap` methods within the spring webflux framework. External calls such as `serviceB` and `serviceC` in the example below would not get reported to New Relic:
```
    return serviceA.getData()
                .map(service -> Response.builder().service(service).build())
                .flatMap(serviceB::getData)
                .flatMap(serviceC::getData)
                .doOnNext(this::saveResponse);
```
* [Adds support for Play 2.16.3](https://github.com/newrelic/newrelic-java-agent/pull/97). Many thanks to [junder31](https://github.com/junder31) for this contribution.

### Fixes:
* [Prevents the agent from logging a timeout exception when using New Relic Edge with Infinite Tracing but the agent hasn’t sent spans in a while](https://github.com/newrelic/newrelic-java-agent/pull/76).
* [Prevents the agent from logging a socket exception when trying to retrieve cloud provider information in a non-cloud environment](https://github.com/newrelic/newrelic-java-agent/pull/80).
* [Adds New Relic EU certifications](https://github.com/newrelic/newrelic-java-agent/pull/89) if [`ca_bundle_path` is specified](https://docs.newrelic.com/docs/agents/java-agent/configuration/configuring-your-ssl-certificates).
This fix also came with the reintroduction of the `use_private_ssl` config, which can be set to add our agent certs to the truststore.

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 6.1.0 (2020-09-30)
* Support for Java 15
* [Ability to add attributes to segments](https://github.com/newrelic/newrelic-java-agent/pull/67)
* `Newrelic.addCustomParameter()` API [now supports boolean values](https://github.com/newrelic/newrelic-java-agent/pull/70)
* [Fix a config issue](https://github.com/newrelic/newrelic-java-agent/pull/39) where the agent would try to read environment
variables using system-property syntax. It will now log the correct syntax and ignore the incorrect config
* [The Java agent now includes the newrelic.com SSL certificate](https://github.com/newrelic/newrelic-java-agent/pull/54).
In previous agent versions, applications using a custom Truststore would have to provide their certificate or use the
`use_private_ssl` configuration which was removed in 6.0.0.
* [Fixed an issue](https://github.com/newrelic/newrelic-java-agent/pull/65) where applications could fail to start due to the agent attempting to access the JMX MBean server before it was initialized.

## Version 6.0.0 (2020-08-26)
* Fix for [asynchronous jar file collection](https://github.com/newrelic/newrelic-java-agent/pull/20).  Collection
of modules will no longer block the initial first harvest.
* Support for [okhttp 4.4](https://github.com/newrelic/newrelic-java-agent/pull/16) instrumentation.
* Fix for `reactor-netty` [verifier versions](https://github.com/newrelic/newrelic-java-agent/pull/15)
* [Improve reconnect behavior](https://github.com/newrelic/newrelic-java-agent/pull/14) by *not* pinning to preconnected collector host instance.
* Fix race condition around [connection pool exhaustion](https://github.com/newrelic/newrelic-java-agent/pull/14) by separating pool timeout from connection timeout. 
* Register an [MBean](https://github.com/newrelic/newrelic-java-agent/pull/28) to expose the agent linking metadata.

## Version 5.14.0 (2020-07-27)

Final closed-source release.  
See [the external release notes](https://docs.newrelic.com/docs/release-notes/agent-release-notes/java-release-notes/java-agent-5140).
