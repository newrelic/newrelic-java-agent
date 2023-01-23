# JBoss Logging

JBoss Logging is a "logging bridge" that integrates with various logging frameworks.

JBoss Logging bridges to the following backends:
- JBoss LogManager (usually only used with WildFly app server)
- Log4j 2
- Log4j 1
- Slf4j
- JDK logging (aka JUL)

This means that when using the `org.jboss.logging:jboss-logging` API, this instrumentation will only apply if the
app is also configured to use the JBoss Logging subsystem (default in JBoss/Wildfly servers) which utilizes the JBoss LogManager.
If the backend is any other logging framework (e.g. log4j) then the instrumentation for that framework will apply instead. 

This instrumentation weaves `org.jboss.logmanager.Logger` (which extends `java.util.logging.Logger`) 
to generate logging metrics and forward log events. However, this instrumentation does not do local log
decorating as that functionality is already handled by the JUL instrumentation module.  

## Testing

Because this instrumentation requires a fully configured JBoss logging subsystem it is not feasible to test it in instrumentation tests.
Instead, the tests can be found in the logging.py AIT which stands up a Wildfly server.
