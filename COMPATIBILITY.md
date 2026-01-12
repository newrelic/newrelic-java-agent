# Java Agent Compatibility

## JVMs

This version of the Java Agent supports Java versions 8 - 25.

  ## App/Web severs
  The agent automatically instruments the following app/web servers.

  * Glassfish 3.0 to latest
  * JBoss 7.0 to latest
  * Jetty 7.0.0 to latest
  * Mule 3.4.0 to latest
  * Netty 3.3.0.Alpha1 to 5.0.0.Alpha1 (exclusive)
  * Netty Reactor 0.7.0.RELEASE to latest
  * Open Liberty 21.0.0.12 to latest
  * Play 2.3.0 to latest
  * Resin 3.1.9 to latest
  * Spray 1.3.1 to latest
  * Tomcat 7.0.0 to latest
  * Undertow 1.1.0.Final to latest
  * WebLogic 12.1.2.1 to 14.1.1
  * WebSphere 8 to 9 (exclusive)
  * WebSphere Liberty 8.5 to latest
  * Wildfly 8.0.0.Final to latest

  ## Frameworks
  The agent automatically instruments the following frameworks.

  * Akka 2.4.10 to latest
  * AWS Firehose 2.1.0 to latest
  * AWS Kinesis 1.11.106 to latest
  * AWS Lambda 1.11.280 to latest
  * AWS S3 1.9.0 to latest
  * AWS SNS 1.11.12 to latest
  * AWS SQS 1.10.44 to latest
  * Cats Effect 3.3.0 to 3.3.4 (exclusive)
  * Cats Effect v2 2.1 to 3.0 (exclusive)
    * Scala 2.12: 2.1 to 3.0 (exclusive)
    * Scala 2.13: 2.1 to 3.0 (exclusive)
  * Cats Effect v3 3.2 to 3.3 (exclusive)
    * Scala 2.12: 3.2 to 3.3.4
    * Scala 2.13: 3.2 to 3.3.4
  * CXF 2.1.3 to latest
  * EJB 6.0 to latest
  * Grails 1.3.0 to 3.0.0.RC1 (exclusive)
  * GraphQL 16.0 to 21.0 (exclusive)
  * GraphQL Java 21.0 to latest
  * Hibernate 3.3.0.CR1 to 6.0.0.Alpha2 (exclusive)
  * Hystrix 1.3.15 to latest
  * JAX-RS 1.0 to 4.0 (exclusive)
  * JCache API 1.0.0 to latest
  * Jersey 1.0.1 to 3.1 (exclusive)
  * Jersey Client 0.9 to latest
  * Micronaut 1.0.0 to latest
  * Monix tasks 2.0.0 to latest
    * Scala 2.12: 2.0.6 to latest
    * Scala 2.13: 3.0.0 to latest
    * Scala 3: 3.4.0 to latest
  * Pekko 1.0.0 to latest
  * Pekko Http 1.0.0 to latest
    * Scala 2.13: 1.0.0 to latest
    * Scala 3: 1.0.0 to latest
  * Pekko Http Core 1.0.0 to latest
    * Scala 2.13: 1.0.0 to latest
    * Scala 3: 1.0.0 to latest
  * Play 2.3.0 to latest
  * Quartz Scheduler 1.7.2 to latest
  * RESTEasy 2.2-RC-1 to latest
  * Servlet 2.3 to latest
  * Spray 1.3.1 to latest
  * Spring 3.0.0.RELEASE to latest
  * Spring AOP 2.0.3 to latest
  * Spring Batch 4.0.0.RELEASE to 6.0.0 (exclusive)
  * Spring Boot Actuator 3.0.0 to 4.0.0-M1 (exclusive)
  * Spring Web Services 1.5.7 to 4.0.0 (exclusive)
  * Spring WebFlux 5.0.0.RELEASE to latest
  * Struts 2.0 to latest
  * Thrift 0.8.0 to latest
  * Vertx 3.2.0 to 5.0.0.CR1 (exclusive)
  * ZIO 1.0.9 to latest
    * Scala 2.13: 1.0.9 to latest
  * JSF (Java Server Faces)

  ## HTTP libraries
  The agent automatically instruments the following HTTP libraries.

  * Akka Http 2.4.5 to latest
  * Akka Http Core 0.4 to latest
  * Apache HttpAsyncClient 4.1 to latest
  * Apache Httpclient 3.1-rc1 to latest
  * Async Http Client 2.0.0-RC1 to latest
  * Blaze Client 0.21 to 0.24.0 (exclusive)
    * Scala 2.12: 0.21.0 to 0.24.0 (exclusive)
    * Scala 2.13: 0.21.0 to 0.24.0 (exclusive)
  * Blaze Server 0.21 to 0.24.0 (exclusive)
    * Scala 2.12: 0.21.0 to 0.24.0 (exclusive)
    * Scala 2.13: 0.21.0 to 0.24.0 (exclusive)
  * Ember Client 0.23.0 to 0.24.0 (exclusive)
  * Ember Server 0.23.0 to 0.24.0 (exclusive)
  * gRPC 1.4.0 to latest
  * HttpUrlConnection 0 to latest
  * Java HttpClient 11 to latest
  * Ning AsyncHttpClient 1.0 to 2.0.0 (exclusive)
  * OKHttp 3.6.0 to 4.4.0 (exclusive)
  * Play WS 2.6.0 to latest
    * Scala 2.13: 2.7.3 to latest
    * Scala 2.12: 2.6.0 to latest
  * Spray 1.3.1 to latest
  * Spring Webclient 5.0.0.RELEASE to latest
  * Spring Webflux 5.0.0.RELEASE to latest
  * STTP 2.2.3 to latest
    * Scala 2.12: 2.2.3 to latest
    * Scala 2.13: 2.2.3 to latest

  ## Logging libraries
  The agent automatically instruments the following logging libraries. For more information about the Java agent's logging solutions, including
  log forwarding and logs in context, see our [logging-specific documentation](https://docs.newrelic.com/docs/logs/logs-context/java-configure-logs-context-all/).

  * Glassfish JUL Extension 7.0.0 to latest
  * Java Logging 8 to latest
  * JBoss Logging 1.3.0 to latest
  * Log4j Layout Template JSON 2.14.0 to latest
  * Log4j-1 1.2.17 to latest
  * Log4j2 2.6 to latest
  * Logback 1.1.0 to latest

  ## Messaging
  The agent automatically instruments the following messaging services.

  * ActiveMQClient 5.8.0 to latest
  * AWS SNS 1.11.12 to latest
  * AWS SQS 1.10.44 to latest
  * Azure Service Bus 7.15.0 to latest
  * JMS 1.1 to latest
  * RabbitAMQP 1.7.2 to latest
  * Spring JMS 0 to latest
  * Spring Kafka 2.2.0.RELEASE to latest

  The agent instruments the following Kafka libraries. Not all Kafka instrumentation is enabled by default. See our [Kafka documentation](https://docs.newrelic.com/docs/apm/agents/java-agent/instrumentation/java-agent-instrument-kafka-message-queues/) for more information.

  * Kafka Clients Config 1.1.0 to latest
  * Kafka Clients Heartbeat 0.10.1.0 to 2.5.0 (exclusive)
  * Kafka Clients Metrics 0.10.0.0 to 4.0.0 (exclusive)
  * Kafka Clients Node Metrics 1.0.0 to latest
  * Kafka Clients Spans 0.11.0.0 to latest
  * Kafka Clients Spans Consumer 2.0.0 to 4.0.0 (exclusive)
  * Kafka Connect Metrics 1.0.0 to latest
  * Kafka Connect Spans 2.0.0 to latest
  * Kafka Streams Metrics 1.0.0 to latest
  * Kafka Streams Spans 2.0.0 to latest


  ## Datastores
  New Relic currently supports MySQL and PostgreSQL to capture explain plans for slow database queries.

  * Generic JDBC (any JDBC compliant driver)
  * Anorm 2.3-M1 to 2.5 (exclusive)
  * AWS v1 DynamoDB 1.11.106 to latest
  * AWS v2 DynamoDB 2.1.0 to latest
  * Cassandra 3.0.0 to latest
  * Couchbase 2.4.0 to latest
  * DB2 9.1 to latest
  * Derby 10.11.1.1 to latest
  * Flyway 8.0.0 to latest
  * H2 1.0.57 to latest
  * H2 R2DBC 0 to latest
  * HikariCP 2.4.0 to latest
  * HSQLDB 1.7.2.2 to latest
  * Jedis 1.4.0 to latest
  * jTDS 1.2 to latest
  * Lettuce 4.2.1.Final to latest
  * MariaDB Java Client (3.0.2-rc,) to latest
  * MariaDB R2DBC 1.0.2 to latest
  * Merlia 7.03 to latest
  * MongoDB 4.2.0 to 5.6.0 (exclusive)
  * MongoDB async clients 3.4.0 to 4.2.0 (exclusive)
  * MongoDB sync clients 3.1.0-rc0 to latest
  * MSSQL R2DBC 0.8.0 to latest
  * MySQL 3.0.8 to latest
  * MySQL R2DBC 0.8.2 to latest
  * Oracle JDBC 5 to latest
  * Oracle R2DBC 0.0.0 to latest
  * Oranxo 3.06 to latest
  * PostgreSQL 8.0-312.jdbc3 to latest
  * PostgreSQL R2DBC 0.9.0 to latest
  * Slick 3.0.0 to latest
  * Solr 4.0.0 to latest
  * Spymemcached 2.11 to latest
  * SQLServer jdk6 to latest
  * Sybase 6 to latest
  * Vertx Sql Client 4.4.2 to 5.0.0 (exclusive)

  ## Instance-level database information
  New Relic collects instance details for a variety of databases and database drivers.

  * Any [compatible JDBC driver](#JDBC)
  * AWS v1 DynamoDB 1.11.106 to latest
  * AWS v2 DynamoDB 2.1.0 to latest
  * Cassandra 3.0.0 to latest
  * Jedis 1.4.0 to latest
  * MongoDB async clients 3.4.0 to 4.2.0 (exclusive)
  * MongoDB sync clients 3.1.0-rc0 to latest
  * Spymemcached 2.11 to latest

  ## AI Monitoring
  If you have version 8.12.0 or higher of Java agent, you can collect AI data from certain AI libraries and frameworks.
  * AWS Bedrock 2.20.157 to latest

  ## Other instrumented features
  * AWS Wrap 0.7.0 to latest
  * Java Completable futures 8 to latest
  * Java Process 8 to latest
  * JSP 2.0 to latest
  * OpenEJB 3.0 to latest
  * OpenJPA 1.0 to latest
  * Scala 2.1.5 to latest
