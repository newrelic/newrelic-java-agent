# Changelog
Noteworthy changes to the agent are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
* TBD

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
* New Relic recommends that you upgrade the agent regularly and at a minimum every 3 months. As of this release, the oldest supported version is [4.8.0](https://docs.newrelic.com/docs/release-notes/agent-release-notes/java-release-notes/java-agent-480).

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