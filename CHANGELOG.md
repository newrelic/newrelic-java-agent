# Changelog
Noteworthy changes to the agent are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
* TBD

## Version 7.11.1 (2022-11-15)

## New features and improvements

## Fixes
* Fix bug with log4j2 metrics https://github.com/newrelic/newrelic-java-agent/pull/1068
* Adds a previously missing delimiter in POSTGRES_DIALECT_PATTERN "|" https://github.com/newrelic/newrelic-java-agent/pull/1050
* Update snakeyaml lib to v1.33 security patch https://github.com/newrelic/newrelic-java-agent/pull/1077


## Deprecation notice
The following instrumentation modules have been deprecated for removal:
- cassandra-datastax-2.1.2
- httpclient-3.0
- jdbc-embedded-derby-10.10.1.1
- jetty-7
- jetty-7.6
- jetty-9
- jetty-9.0.4
- jetty-9.1
- mongodb-2.12
- mongodb-2.14
- mongodb-3.0
- okhttp-3.0.0
- okhttp-3.4.0
- okhttp-3.5.0

The http.responseCode, response.status and response.statusMessage transaction/span attributes are deprecated and will be removed in a future release. These have been replaced by http.statusCode and http.statusText.

**Full Changelog**: https://github.com/newrelic/newrelic-java-agent/compare/v7.11.0...v7.11.1

## Version 7.11.0 (2022-10-27)

### New features and improvements

* Support Java 19 [1022](https://github.com/newrelic/newrelic-java-agent/pull/1022)
* Support Play 2.8.16+ [981](https://github.com/newrelic/newrelic-java-agent/pull/981)
* Support ojdbc8 v21.1.0.0+ [1042](https://github.com/newrelic/newrelic-java-agent/pull/1042)
* Support Semeru/OpenJ9 JVMs [993](https://github.com/newrelic/newrelic-java-agent/pull/993)
* Support log forwarding for java.util.logging (JUL) [1049](https://github.com/newrelic/newrelic-java-agent/pull/1049)

#### Support forwarding log context data [866](https://github.com/newrelic/newrelic-java-agent/pull/866)
The agent can now forward data in Mapped Diagnostic Context (MDC, logback/slf4j) and ThreadContext (log4j2) as attributes when forwarding log records. When the feature is enabled, these attributes will be added to the log records with a `context.` prefix. For details on how to enable this feature see the documentation for [context_data](https://docs.newrelic.com/docs/apm/agents/java-agent/configuration/java-agent-configuration-config-file/#logging-context-data).

#### Custom Event Limit Increase  [1036](https://github.com/newrelic/newrelic-java-agent/pull/1036)
  This version increases the default limit of custom events from 10,000 events per minute to 30,000 events per minute. In the scenario that custom events were being limited, this change will allow more custom events to be sent to New Relic. There is also a new configurable maximum limit of 100,000 events per minute. To change the limits, see the documentation for [max_samples_stored](https://docs.newrelic.com/docs/apm/agents/java-agent/configuration/java-agent-configuration-config-file/#Custom_Events). To learn more about the change and how to determine if custom events are being dropped, see our Explorers Hub post.

#### Code-level metrics on by default [1037](https://github.com/newrelic/newrelic-java-agent/pull/1037)
  The code-level metrics functionality introduced with agent 7.10 is now enabled by default. This feature will cause an increase in the consumption of data. The amount depends on the application. To disable code-level metrics, see instructions on our [code-level metrics documentation](https://docs.newrelic.com/docs/apm/agents/java-agent/configuration/java-agent-configuration-config-file#code-level-metrics).

### Fixes
* Prevent an exception from being thrown in the Jedis instrumentation [1011](https://github.com/newrelic/newrelic-java-agent/pull/1011)
* Improvement on Http4s transactions [1006](https://github.com/newrelic/newrelic-java-agent/pull/1006)
* Fix noticeError API not verifying whether errors were expected [1014](https://github.com/newrelic/newrelic-java-agent/pull/1014)
* Add operation for Lettuce queries to clusters [1031](https://github.com/newrelic/newrelic-java-agent/pull/1031)
* Fix exception when building up the agent jar from a clean repo [1048](https://github.com/newrelic/newrelic-java-agent/pull/1048)
* Better error handling for code-level metrics [1021](https://github.com/newrelic/newrelic-java-agent/pull/1021) [1051](https://github.com/newrelic/newrelic-java-agent/pull/1051)
* Fix HttpUrlConnection spans not terminating on exception [1053](https://github.com/newrelic/newrelic-java-agent/pull/1053)

### Deprecation notice
The following instrumentation modules are being deprecated for removal:
* cassandra-datastax-2.1.2
* httpclient-3.0
* jdbc-embedded-derby-10.10.1.1
* jetty-7
* jetty-7.6
* jetty-9
* jetty-9.0.4
* jetty-9.1
* mongodb-2.12
* mongodb-2.14
* mongodb-3.0
* okhttp-3.0.0
* okhttp-3.4.0
* okhttp-3.5.0

The http.responseCode, response.status and response.statusMessage transaction/span attributes are deprecated and will be removed in a future release. These have been replaced by http.statusCode and http.statusText 513

**Full Changelog**: https://github.com/newrelic/newrelic-java-agent/compare/v7.10.0...v7.11.0


## Version 7.10.0 (2022-09-13)

### New Features and Improvements

#### Added the following Jakarta EE 9/9.1 compatible instrumentation:

Jetty 11
Tomcat 10
Enterprise Java Beans 4.0
Jakarta RS/WS
Jersey 3+
Jersey Client 3
JSP 3
Servlet 5 & 6
Jakarata.xml
JMS 3
Glassfish 6.0
Open Liberty 21.0.0.12+

#### Code level metrics

For traced methods in automatic instrumentation or from @Trace annotations, the agent is now capable of reporting metrics with method-level granularity. When the new functionality is enabled, the agent will associate source-code-related metadata with some metrics. Then, when the corresponding Java class file that defines the methods is loaded up in a [New Relic CodeStream](https://www.codestream.com/)-powered IDE, [the four golden signals](https://sre.google/sre-book/monitoring-distributed-systems/) for each method will be presented to the developer directly.

#### Agent log forwarding now adds the following attributes to log events for the log4j2 and logback1.2 frameworks:

thread.name
thread.id
logger.name
logger.fqcn
error.class
error.stack
error.message

### Fixes
Fixed an issue with Distributed Tracing headers not being added on external requests made with the HttpUrlConnection client

### Support statement
New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and performance benefits. Additionally, older releases will no longer be supported when they reach [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

**Full Changelog**: https://github.com/newrelic/newrelic-java-agent/compare/v7.9.0...v7.10.0

## Version 7.9.0 (2022-08-28)

### New features and improvements
* Where applicable, existing instrumentation has been tested and verified as compatible with Jakarta EE 8. [900](https://github.com/newrelic/newrelic-java-agent/pull/900)
* Add new instrumentation to support Jetty 10. [936](https://github.com/newrelic/newrelic-java-agent/pull/936)
* Update to jfr-daemon 1.9.0 to address [CVE-2020-29582](https://github.com/advisories/GHSA-cqj8-47ch-rvvq) and improve CPU overhead. [937](https://github.com/newrelic/newrelic-java-agent/pull/937)
* Add support to pass a boolean environment variable `NEWRELIC_DEBUG` where setting it to `true` activates the debug configuration. [890](https://github.com/newrelic/newrelic-java-agent/pull/890)
* Improved performance by internally replacing regex replace with iterative char replace (thanks to @zowens for this contribution) [933](https://github.com/newrelic/newrelic-java-agent/pull/933)

### Fixes
* Update the `httpurlconnection` instrumentation to use newer distributed tracing APIs so that spans are correctly marked as external calls in distributed traces and contain the expected `http.*` attributes. [885](https://github.com/newrelic/newrelic-java-agent/pull/885)
* Illegal Access Exception is no longer thrown from apps using NR agent with scala 2.12 and Java 11. [876](https://github.com/newrelic/newrelic-java-agent/pull/876)

### Support statement
New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and performance benefits. Additionally, older releases will no longer be supported when they reach [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 7.8.0 (2022-06-16)

### New features and improvements
* Updated the agent to use caffeine 2.9.3 [832](https://github.com/newrelic/newrelic-java-agent/pull/832)
* Refactored the `log.level` attribute name on LogEvents to instead be `level` [858](https://github.com/newrelic/newrelic-java-agent/pull/858)
* Kafka instrumentation - supports metrics for kafka-clients versions 3.x. [860](https://github.com/newrelic/newrelic-java-agent/pull/860) and [865](https://github.com/newrelic/newrelic-java-agent/pull/865)
* Update to jfr-daemon 1.8.0 [869](https://github.com/newrelic/newrelic-java-agent/pull/869)
* Lettuce instrumentation - supports lettuce-core 4.3 up to 6.x.  Please remove any other (experimental/incubating) lettuce extensions or else Redis database metrics could be doubled. [872](https://github.com/newrelic/newrelic-java-agent/pull/872)

### Fixes
* Fixed CQLParser `getOperationAndTableName` exception handling. Exceptions are now handled within the `CQLParser`. [857](https://github.com/newrelic/newrelic-java-agent/pull/857)
* Removed akka-http-core bindAndHandle instrumentation to resolve scenarios where duplicated transactions could result [850](https://github.com/newrelic/newrelic-java-agent/pull/850) (see [Scala Akka HTTP core instrumentation](https://docs.newrelic.com/docs/apm/agents/java-agent/frameworks/scala-akka-http-core/) for more details)

### Support statement:

* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and performance benefits. Additionally, older releases will no longer be supported when they reach [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).


## Version 7.7.0 (2022-05-03)

### New features and improvements

* Supports Java 18 [813](https://github.com/newrelic/newrelic-java-agent/pull/813)
* APM logs in context. Automatic application log forwarding is now enabled by default. This version of the agent will automatically send enriched application logs to New Relic. To learn more about about this feature see [here](/docs/logs/logs-context/java-configure-logs-context-all), and additional configuration options are available [here](/docs/apm/agents/java-agent/configuration/java-agent-configuration-config-file/#Logs-in-Context). To learn about how to toggle log ingestion on or off by account see [here](/docs/logs/logs-context/disable-automatic-logging). [817](https://github.com/newrelic/newrelic-java-agent/pull/817)
* Added instrumentation support for the Postgres, MySQL, Oracle & MSSQL R2DBC connectors [810](https://github.com/newrelic/newrelic-java-agent/pull/810) [816](https://github.com/newrelic/newrelic-java-agent/pull/816) [829](https://github.com/newrelic/newrelic-java-agent/pull/829) [828](https://github.com/newrelic/newrelic-java-agent/pull/828)

### Fixes

* Patches a security issue related to an older version of jszip that is included in the Java agent API Javadoc jar [820](https://github.com/newrelic/newrelic-java-agent/pull/820)

### Support statement:

* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and performance benefits. Additionally, older releases will no longer be supported when they reach [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).


## Version 7.6.0 (2022-04-04)

### New features and improvements
* Added built-in agent support for Logs in Context (log4j 2.6+ and Logback 1.1+). Read more about this capability on our [Logs in Context feature page](https://docs.newrelic.com/docs/logs/logs-context/java-configure-logs-context-all/) [718](https://github.com/newrelic/newrelic-java-agent/pull/718)
* Added instrumentation support for the MariaDB & H2 R2DBC connectors [799](https://github.com/newrelic/newrelic-java-agent/pull/799) [724](https://github.com/newrelic/newrelic-java-agent/pull/724)
* Updated agent support for Jedis 4.0.0+ [698](https://github.com/newrelic/newrelic-java-agent/pull/698)
* Updated agent support for Cassandra dataStax 4+ [690](https://github.com/newrelic/newrelic-java-agent/pull/690)

### Fixes
* Guard against intermittent null pointer exceptions [707](https://github.com/newrelic/newrelic-java-agent/pull/707)
* Support CSP nonce parameter for RUM header and footer [591](https://github.com/newrelic/newrelic-java-agent/pull/591)
* Fixed an issue with auto app naming and distributed tracing transactions [566](https://github.com/newrelic/newrelic-java-agent/pull/566)
* Increased maximum `TransactionError` message size [581](https://github.com/newrelic/newrelic-java-agent/issues/581)
* The `http.responseCode`, `response.status` and `response.statusMessage` transaction/span attributes are deprecated and will be removed in a future release. These have been replaced by `http.statusCode` and `http.statusText` [513](https://github.com/newrelic/newrelic-java-agent/pull/513):

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).


## Version 7.5.0 (2022-01-12)

## New features and improvements

* Update to jfr-daemon 1.7.0 - fixes a memory leak condition by cleaning up copies of JFR recordings. Also updated to use version 0.13.1 of the telemetry-sdk [638](https://github.com/newrelic/newrelic-java-agent/pull/638)
* Update HTTP response code attribute names. This will add `http.statusCode` and `http.statusText` to spans and transactions [513](https://github.com/newrelic/newrelic-java-agent/pull/513)
* Provide support for Datastax/Cassandra WrappedStatments. [525](https://github.com/newrelic/newrelic-java-agent/pull/525)
* Add instrumentation for gRPC 1.40.0+ (thanks to [fcaylus](https://github.com/fcaylus) for this contribution) [518](https://github.com/newrelic/newrelic-java-agent/pull/518)
* Add suppressed exceptions to ThrowableError.stackTrace (thanks to [dkarlinsky](https://github.com/dkarlinsky) for this contribution)  [405](https://github.com/newrelic/newrelic-java-agent/pull/405)
* Add Scala instrumentation and Scala API for Monix Tasks [543](https://github.com/newrelic/newrelic-java-agent/pull/543)
* Add instrumentation for Mongo async/reactivestreams drivers versions 3.4.x to 4.1.x [609](https://github.com/newrelic/newrelic-java-agent/pull/609)
* Scala Cats Effect 3 - modified the API to support passing the transaction by implicit reference, rather than using ThreadLocal variables [578](https://github.com/newrelic/newrelic-java-agent/pull/578)
* Add Instrumentation for Play WS 2.6.0 under Scala 2.13 [594](https://github.com/newrelic/newrelic-java-agent/pull/594)
* Agent optimization: change String.replaceAll in favor of Pattern.compile (thanks to [brunolellis](https://github.com/brunolellis) for this contribution) [592](https://github.com/newrelic/newrelic-java-agent/pull/592)

## Fixes
* Upgrade log4j-core version to 2.17.1 to address security vulnerability [CVE-2021-44832](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44832) [625](https://github.com/newrelic/newrelic-java-agent/pull/625)
* Enhancements for Spring WebFlux and Reactor Netty instrumentation to address gaps in instrumentation. Also includes support for upgraded Spring Security configurations [538](https://github.com/newrelic/newrelic-java-agent/pull/538)
* Update Async-Http-Client library version to 2.0.35 to address security vulnerability [CVE-2017-14063](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-14063)  [577](https://github.com/newrelic/newrelic-java-agent/pull/577)
* Handle null pointer exceptions in hostname lookup [587](https://github.com/newrelic/newrelic-java-agent/pull/587)
* Properly expire tokens used in CompletableFuture instrumentation to reduce memory usage and prevent telemetry reporting delays [634](https://github.com/newrelic/newrelic-java-agent/pull/634)
* Add additional exception handling to catch ConnectionPoolTimeoutException errors, which may lead to an unrecoverable agent state [637](https://github.com/newrelic/newrelic-java-agent/pull/637)
* Resolve Solr FilterCache Memory Leak [613](https://github.com/newrelic/newrelic-java-agent/pull/613)
* Reintroduce MongoDB sync instrumentation (erroneously removed in a prior release while async support was added) [635](https://github.com/newrelic/newrelic-java-agent/pull/635)
* Fix Sql obfuscation so that it applies correctly to queries with certain formatting [632](https://github.com/newrelic/newrelic-java-agent/pull/632)
* Agent configuration `expected_status_codes` is not honored when a transaction exception is encountered [565](https://github.com/newrelic/newrelic-java-agent/pull/565)
* Scala Cats Effect - ensure Http4s transaction tracer is created on request run. This resolves some invalid tracer states that were causing null pointer exceptions [530](https://github.com/newrelic/newrelic-java-agent/pull/530)
* Fix Akka HTTP bindFlow [555](https://github.com/newrelic/newrelic-java-agent/pull/555)
* Address Caffeine cache causing memory leak and OOM condition  [593](https://github.com/newrelic/newrelic-java-agent/pull/593)

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 7.4.0 (2021-10-28)

### New features and improvements

* Support for Java 17 [#433](https://github.com/newrelic/newrelic-java-agent/pull/433)

* Distributed Tracing is on by Default and deprecates Cross Application Tracing [#486](https://github.com/newrelic/newrelic-java-agent/pull/486)
  - Increases the default maximum number of samples stored for Span Events from 1000 to 2000.
  - The maximum number of samples stored for Span Events can be configured via the max_samples_stored configuration in the newrelic.yml.
  ```
  span_events:
    max_samples_stored: 2000
  ```
  **Notice:** This feature will cause an increase in the consumption of data. The amount depends on the application. This feature can be disabled by adding the following to the agent yaml config nested under the common stanza:
  ```
  distributed_tracing:
    enabled: false
  ```

* Auto-instrumentation Support for GraphQL-Java 17.0+ [#487](https://github.com/newrelic/newrelic-java-agent/pull/487)
* This version tested agent support for the ARM64/Graviton2 platform


### Fixes
The existing MongoDB sync client instrumentation was incorrectly applying when MongoDB reactive or async client was being used, which could lead to segment timeouts and long transaction response times. [#476](https://github.com/newrelic/newrelic-java-agent/pull/476)

### Deprecations/Removed Features
Cross Application Tracing is now deprecated, and disabled by default. To continue using it, enable it with `cross_application_tracer.enabled = true` and `distributed_tracing.enabled = false`.

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).


## Version 7.3.0 (2021-9-30)

### New features and improvements

* Support for [Java 16](https://github.com/newrelic/newrelic-java-agent/pull/366)

* Auto-instrumentation support for [java.net.http.HttpClient](https://github.com/newrelic/newrelic-java-agent/pull/251) 

* Migrate the Agent’s caching library from Guava to [Caffeine](https://github.com/newrelic/newrelic-java-agent/pull/295)
  * [Caffeine provides an in-memory cache](https://github.com/ben-manes/caffeine#cache) using a Google Guava inspired API. The improvements draw on the author’s experience designing Guava's cache and `ConcurrentLinkedHashMap`.
  * We expect this change to provide improvement in cases where we saw thread contention and deadlocks attributable to the Guava library.
 
### Fixes
* Removed support for the [anorm-2.0 instrumentation module](https://github.com/newrelic/newrelic-java-agent/pull/426)
  * The artifacts that this module instrumented are no longer available.

### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).


## Version 7.2.0 (2021-9-9)

### New features and improvements

* Scala Library Instrumentation [#362](https://github.com/newrelic/newrelic-java-agent/pull/362) and [#363](https://github.com/newrelic/newrelic-java-agent/pull/363)
  * STTP versions 2 & 3  Akka-HTTP, HTTP4s and STTP core backends
  * Cats-effect v2
  * ZIO v1
  * HTTP4s client & server v0.21
  * Play 2.3-2.8
  * Akka-HTTP v10.1 & v10.2
  * For more information, see [Scala instrumentation](https://docs.newrelic.com/docs/agents/java-agent/frameworks/scala-installation-java/).

* Scala API support (see PRs above)
  * Scala APIs provided for explicit instrumentation of several of above libraries in case auto-instrumentation is not desired
  * Cats-effect v2
  * ZIO v1
  
* AWS v2 DynamoDB Instrumentation [#343](https://github.com/newrelic/newrelic-java-agent/pull/343)
  * Synchronous and asynchronous AWS v2 APIs are auto-instrumented similarly to v1 APIs
  * For more information, see [Add support for AWS SDK 2 DynamoDB sync/async clients](https://github.com/newrelic/newrelic-java-agent/issues/246)
  
* GraphQL 16 Instrumentation [#396](https://github.com/newrelic/newrelic-java-agent/pull/396)
  * Create meaningful transaction names
  * Create meaningful spans
  * Reporting GraphQL errors
  * For more information, see [GraphQL for Java](https://github.com/newrelic/newrelic-java-agent/issues/356)

* JFR feature causing excessive overhead when enabled [JFR #203](https://github.com/newrelic/newrelic-jfr-core/issues/203)
  * Refactored code to use less memory.

### Fixes
The existing MongoDB instrumentation was partially applying when MongoDB Reactive Streams is being used.

* Disable weaving package when MongoDB 4.x+ reactive driver detected [#341](https://github.com/newrelic/newrelic-java-agent/pull/341)
  * For more information, see [Spring Reactive DB Drivers - MongoDB Support](https://github.com/newrelic/newrelic-java-agent/issues/198)
  
### Support statement:
* New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and
  performance benefits. Additionally, older releases will no longer be supported when they reach
  [end-of-life](/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

## Version 7.1.1 (2021-7-15)

 Due to overhead caused in some applications [Real-time profiling for Java using JFR metrics](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-profiling-java-using-jfr-metrics/)
is now disabled by default.

It can be enabled following the instructions [here](#enabling-jfr).


## Version 7.1.0 (2021-7-7)

### Known issue
Some customers saw increased overhead when Real-time profiling is enabled.
See instructions to disable Real-time profiling in the [notice](#disabling-jfr) below.

### New features and improvements:

* Java instrumentation by XML new properties [#288](https://github.com/newrelic/newrelic-java-agent/pull/288)
  * traceLambda - to trace lambdas inside a method
  * traceByReturnType - to trace all methods in a class that return a given type

  These are compatible with Java and Scala. For more information, see
  [Java instrumentation by XML](https://docs.newrelic.com/docs/agents/java-agent/custom-instrumentation/java-instrumentation-xml/).


* Scala APIs [#254](https://github.com/newrelic/newrelic-java-agent/pull/254)

  New artifacts allow Scala code to be instrumented using a fluent Scala API
  instead of the Java annotations. There are specific artifacts for versions
  2.10, 2.11, 2.12, 2.13 of Scala. Scala 3.0 users can use the 2.13 artifact.

  For more information, see
  [Scala instrumentation](https://docs.newrelic.com/docs/agents/java-agent/frameworks/scala-installation-java/).


* Real-time profiling for Java using JFR metrics  [#333](https://github.com/newrelic/newrelic-java-agent/pull/333)

  [Real-time profiling for Java using JFR metrics](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-profiling-java-using-jfr-metrics/)
  is now enabled by default.

  **Notice:** this feature will cause an increase in the consumption of data.
  The amount depends on the application. <a id="disabling-jfr"></a> It can be disabled by adding the
  following to the agent yaml config nested under the `common` stanza:
  
  ```
    jfr:
      enabled: false
  ```
  For more information, see
  [JFR core README](https://github.com/newrelic/newrelic-jfr-core/blob/main/README.md).

## Version 7.0.1 (2021-6-15)

### Fixes

* Fixes an issue where the agent would break OkHttp versions 3.X and lower. [#324](https://github.com/newrelic/newrelic-java-agent/issues/324)


## Version 7.0.0 (2021-6-9)

### New features and improvements:

* Real-time profiling for Java using JFR metrics  
  [Real-time profiling for Java using JFR metrics](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-profiling-java-using-jfr-metrics/) is now fully integrated into the Java agent. See the [JFR core README](https://github.com/newrelic/newrelic-jfr-core/blob/main/README.md) for additional details.
  
  <a id="enabling-jfr"></a>
  This feature requires a supported version of Java (Java 8 (specifically version `8u262`+) or Java 11+) and is currently disabled by default. To enable it set the following in your yaml (indented 2 spaces under the common stanza).

    ```
      jfr:
        enabled: true
    ```
    
    **Notice:** If you were previously using the [jfr-daemon jar](https://github.com/newrelic/newrelic-jfr-core) as an agent extension or standalone process you should remove that option to avoid potential conflicts with the JFR service that is now built into the agent.

* Not compatible with Java 7  
  In order to continue to innovate and efficiently provide new capabilities to our customers who run on the JVM, this and future agent versions are not compatible with Java 7. If you are running Java 7, you may continue to use Java agent 6.5.0 or lower. For details, see this topic on the [Explorers Hub](https://discuss.newrelic.com/t/important-upcoming-changes-to-capabilities-across-synthetics-apm-java-php-infrastructure-mobile-agents-health-maps-statsd-and-legacy-dashboards/147982).

* Adds support for akka http with Scala 2.13 [#271](https://github.com/newrelic/newrelic-java-agent/pull/271)

* Class annotation to trace lambda methods [#274](https://github.com/newrelic/newrelic-java-agent/pull/274)

* Class annotation to trace methods by return type [#275](https://github.com/newrelic/newrelic-java-agent/pull/275)

### Fixes:

* Fixes an issue that could cause multiple versions of `akka-http-core` instrumentation to apply at the same time. [#208](https://github.com/newrelic/newrelic-java-agent/pull/208)

* The agent will now log dropped events at `FINE` instead of `WARN` to decrease verbosity. [#296](https://github.com/newrelic/newrelic-java-agent/pull/296)

* Fixes Javadoc comments that incorrectly stated that, when calling the `noticeError` API multiple times, the first error would be reported when in fact it is the last error that is reported. [#313](https://github.com/newrelic/newrelic-java-agent/pull/313)

### Support statement:

New Relic recommends that you upgrade the agent regularly to ensure that you're getting the latest features and performance benefits. Additionally, older releases will no longer be supported when they reach [end-of-life](https://docs.newrelic.com/docs/using-new-relic/cross-product-functions/install-configure/notification-changes-new-relic-saas-features-distributed-software/).

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

For more information, see the [Explorers Hub post](https://discuss.newrelic.com/t/important-upcoming-changes-to-capabilities-across-synthetics-apm-java-php-infrastructure-mobile-agents-health-maps-statsd-and-legacy-dashboards/147982).

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
