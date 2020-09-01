## Java Attach

In addition to being able to attach to Java VMs using -javaagent, the java agent
jar is executable. It allows customers to run a few different commands such as
recording a deployment. Now it supports attaching to java processes that are
already running using the attach option.

The syntax for running the agent jar to attach looks like this:

    java -jar /newrelic/newrelic.jar attach -license <licensekey>

This tells the agent to attach to all of the running java processes on the machine.
We refer to this process as the `attach` process and refer to the JVMs to which
the agent attaches as `target` processes.  The attach process must use a JDK
and at the time of this writing it should be JDK 7 or 8 - we don't support
Java 9 which moved the `tools.jar` code to the `jdk.attach` module.

In the future this might make it easier to do customer POCs. We could have the
Infra agent detect java processes and automatically attach the agent to them.
We probably also want the Infra agent to provide a Java 8 JDK for the
attach process.

### Environment

Java agent configuration can be set through a configuration file, through
environment variables that start with `NEW_RELIC_` and through system
properties that start with `newrelic.config.`.  When the java agent is run
in attach mode, it gathers all of the New Relic system and environment
properties and creates a JSON string containing this information.  As
it attaches to processes it passes the full path to the agent jar and this
JSON string.  When the JVM attaches an agent to a running process, it passes
the string argument to the agent's `agentmain` method.  Our agent treats the
environment properties in that JSON string as its own system properties/environment.

## Testing

### Running app servers and attaching the java agent

Run the service.  For a large service like WebSphere make sure it fully starts.

    docker-compose up SERVICE

Attach the agent from another terminal shell

    docker exec -it SERVICE java -jar /newrelic/newrelic.jar attach -license <licensekey>

The supported SERVICEs are:

 * jetty9
 * tomcat7
 * tomcat8
 * tomcat9
 * jboss6
 * jboss7
 * wildfly20
 * websphere-liberty (crashes with IBM JVM)
 * springboot-petclinic

### Agent logs

    tail -f ~/newrelic-agent/build/newrelicJar/logs/newrelic_agent.log
