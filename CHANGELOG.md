# Changelog
Noteworthy changes to the agent are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
* TBD

## Version 6.1.0 (2020-09-30)
* Support for Java 15
* [Ability to add attributes to segments](https://github.com/newrelic/newrelic-java-agent/pull/67)
* `Newrelic.addCustomParameter()` API [now supports boolean values](https://github.com/newrelic/newrelic-java-agent/pull/70)
* [Fix a config issue](https://github.com/newrelic/newrelic-java-agent/pull/39) where the agent would try to read environment
variables using system-property syntax. It will now log the correct syntax and ignore the incorrect config
* [The Java agent now includes the newrelic.com SSL certificate](https://github.com/newrelic/newrelic-java-agent/pull/54).
In previous agent versions, applications using a custom Truststore would have to provide their certificate or use the
`use_private_ssl` configuration which was removed in 6.0.0.

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