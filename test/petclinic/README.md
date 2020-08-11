### Attach POC

In addition to being able to attach to Java VMs using `-javaagent`, the java agent jar is executable.  It allows customers to run a few different commands such as recording a deployment.  Now it supports attaching to java processes that are already running using the `attach` option.

This demo builds a docker image of the Spring PetClinic application.  The local agent jar build is copied into the container (in a real world scenario we would use a different mechanism to download the agent).  You can start up the container normally which starts the application without instrumentation and then, in a separate terminal, execute a command in the running container that instructs the agent to attach to all running java processes.

In the future this might make it easier to do customer POCs.  We could have the Infra agent detect java processes and automatically attach the agent to them.

### Try it out

From the root directory of the agent repo, build the java agent (make sure you've followed the directions in the main README to set up your build environment)

    ./gradlew jar --parallel

Run the spring petclinic test app

    docker up springboot-petclinic

Send traffic to the application at http://localhost:8080

In a separate terminal, run the attach command.  If you want to use production simply leave off the `newrelic.config.host` property and update the `newrelic.config.license_key`.

    docker exec -it springboot-petclinic java -Dnewrelic.config.license_key=<licensekey> -jar /newrelic/newrelic.jar attach

Send more traffic to the app.  It should report into the staging Bender account (190).  It might take ~5 minutes for data to show up.

To view the agent log4j

    docker exec -it springboot-petclinic tail -f /root/newrelic/logs/newrelic_agent.log

If you CTRL-C out of running the container you need to remove it.

    docker rm springboot-petclinic

### How attach works

The VirtualMachine class in tools.jar allows us to load an agent into a target VM from another VM.

https://docs.oracle.com/javase/7/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html#loadAgent(java.lang.String,%20java.lang.String)

Once we've identified a VM to which we want to attach, we invoke load agent and pass the full path to the agent jar and a string of options.  The target VM loads the agent in a way that's very similar to using `-javaagent`.  It appears to rejit all of the loaded classes so our instrumentation loads correctly.

Our agent is capable of starting without a configuration file.  The default configuration values can be overwritten using environment variables that start with `NEW_RELIC_` and system properties starting with `newrelic.config.`.  When executing the agent jar using `attach`, it records the key/values for all of our environment and system property variables and serializes them as a json string.  This is passed to the target VM when we invoke `loadAgent`.  As the agent is loaded into the target VM it deserializes the json and wires up everything so that the environment and system properties defined in the attach process is visible to the target process.

### JVM Requirements

It looks like attach is supported by Oracle / OpenJDK JVMs from version 6 up.  To run our java agent jar in attach mode we require a version 6-8 JDK because we need `tools.jar` which is not packaged in the JRE and was removed in Java 9.

### Notes

#### Be patient

It takes a while, maybe 5 minutes, for data to appear in staging.

#### Alpine base image issue

There's a petclinic base image but when I tried to use it the attached failed with

    Attaching the java agent to 1:/petclinic.jar
    com.sun.tools.attach.AttachNotSupportedException: Unable to get pid of LinuxThreads manager thread
    	at sun.tools.attach.LinuxVirtualMachine.<init>(LinuxVirtualMachine.java:86)
    	at sun.tools.attach.LinuxAttachProvider.attachVirtualMachine(LinuxAttachProvider.java:63)
    	at com.sun.tools.attach.VirtualMachine.attach(VirtualMachine.java:208)
    	at com.newrelic.agent.discovery.Discovery.attach(Discovery.java:109)
    	at com.newrelic.agent.discovery.Discovery.access$100(Discovery.java:22)
    	at com.newrelic.agent.discovery.Discovery$2.accept(Discovery.java:96)
    	at com.newrelic.agent.discovery.Discovery$2.accept(Discovery.java:90)
    	at com.newrelic.agent.discovery.Discovery.discover(Discovery.java:72)
    	at com.newrelic.agent.discovery.SafeDiscovery.discoverAndAttach(SafeDiscovery.java:14)
    	at com.newrelic.agent.AgentCommandLineParser.parseCommand(AgentCommandLineParser.java:78)
    	at com.newrelic.agent.Agent.main(Agent.java:291)
    	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    	at java.lang.reflect.Method.invoke(Method.java:498)
    	at com.newrelic.bootstrap.Bootstra

That image used Alpine as a base image.  This demo uses Centos as a base and it works fine.
