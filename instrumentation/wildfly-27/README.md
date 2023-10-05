# Wildfly-27 Instrumentation

This instrumentation module instruments Wildfly 27 and above releases using the `jakarta` namespace (unless there is a version cap in `build.grafdle`). 

The role of this module is to start web transactions and to set the dispatcher version.

However not every aspect of wildfly is supported in this module. For setting the port number, 
CAT response headers, and JMX MBeans, refer to the instrumentation modules:
- wildfly-8-CAT
- wildfly-8-PORT
- wildfly-jmx-14

## How to fix and/or workaround NoClassDefFoundError(s)

There used to be a bug in wildfly 23 and above where if a class gets weaved, 
and we use packages from the Java standard library such as `java.util.logging`, 
we would get a `NoClassDefFoundError`.

You would see a stacktrace that looks like the following:
```text
2022-02-01T11:59:16,167-0800 [97709 221] com.newrelic.agent.instrumentation.ClassTransformerServiceImpl FINEST: An error was thrown from instrumentation library com.newrelic.instrumentation.servlet-2.4
java.lang.NoClassDefFoundError: java/util/logging/Level
	at com.nr.instrumentation.servlet24.ServletHelper.setTxNameUsingServletName(ServletHelper.java:187) ~[?:?]
	at com.nr.instrumentation.servlet24.ServletHelper.setTransactionName(ServletHelper.java:96) ~[?:?]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java) ~[?:?]
	at io.undertow.servlet.handlers.ServletHandler.handleRequest(ServletHandler.java:74) ~[?:?]
	at io.undertow.servlet.handlers.security.ServletSecurityRoleHandler.handleRequest(ServletSecurityRoleHandler.java:62) ~[?:?]
	at io.undertow.servlet.handlers.ServletChain$1.handleRequest(ServletChain.java:68) ~[?:?]
	at io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest(ServletDispatchingHandler.java:36) ~[?:?]
	at org.wildfly.extension.undertow.security.SecurityContextAssociationHandler.handleRequest(SecurityContextAssociationHandler.java:78) ~[?:?]
	at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.RedirectDirHandler.handleRequest(RedirectDirHandler.java:68) ~[?:?]
	at io.undertow.servlet.handlers.security.SSLInformationAssociationHandler.handleRequest(SSLInformationAssociationHandler.java:117) ~[?:?]
	at io.undertow.servlet.handlers.security.ServletAuthenticationCallHandler.handleRequest(ServletAuthenticationCallHandler.java:57) ~[?:?]
	at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.security.handlers.AbstractConfidentialityHandler.handleRequest(AbstractConfidentialityHandler.java:46) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.security.ServletConfidentialityConstraintHandler.handleRequest(ServletConfidentialityConstraintHandler.java:64) ~[?:?]
	at io.undertow.security.handlers.AuthenticationMechanismsHandler.handleRequest(AuthenticationMechanismsHandler.java:60) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler.handleRequest(CachedAuthenticatedSessionHandler.java:77) ~[?:?]
	at io.undertow.security.handlers.NotificationReceiverHandler.handleRequest(NotificationReceiverHandler.java:50) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.security.handlers.AbstractSecurityContextAssociationHandler.handleRequest(AbstractSecurityContextAssociationHandler.java:43) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at org.wildfly.extension.undertow.security.jacc.JACCContextIdHandler.handleRequest(JACCContextIdHandler.java:61) ~[?:?]
	at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at org.wildfly.extension.undertow.deployment.GlobalRequestControllerHandler.handleRequest(GlobalRequestControllerHandler.java:68) ~[?:?]
	at io.undertow.servlet.handlers.SendErrorPageHandler.handleRequest(SendErrorPageHandler.java:52) ~[?:?]
	at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43) ~[undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.ServletInitialHandler.handleFirstRequest(ServletInitialHandler.java:269) ~[undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.ServletInitialHandler.access$100(ServletInitialHandler.java:78) ~[undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.ServletInitialHandler$2.call(ServletInitialHandler.java:133) ~[undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.ServletInitialHandler$2.call(ServletInitialHandler.java:130) ~[undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.core.ServletRequestContextThreadSetupAction$1.call(ServletRequestContextThreadSetupAction.java:48) ~[undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.core.ContextClassLoaderSetupAction$1.call(ContextClassLoaderSetupAction.java:43) ~[undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at org.wildfly.extension.undertow.security.SecurityContextThreadSetupAction.lambda$create$0(SecurityContextThreadSetupAction.java:105) ~[?:?]
	at org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService$UndertowThreadSetupAction.lambda$create$0(UndertowDeploymentInfoService.java:1530) ~[?:?]
	at org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService$UndertowThreadSetupAction.lambda$create$0(UndertowDeploymentInfoService.java:1530) ~[?:?]
	at org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService$UndertowThreadSetupAction.lambda$create$0(UndertowDeploymentInfoService.java:1530) ~[?:?]
	at org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService$UndertowThreadSetupAction.lambda$create$0(UndertowDeploymentInfoService.java:1530) ~[?:?]
	at io.undertow.servlet.handlers.ServletInitialHandler.dispatchRequest(ServletInitialHandler.java:249) [undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.ServletInitialHandler.access$000(ServletInitialHandler.java:78) [undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.servlet.handlers.ServletInitialHandler$1.handleRequest(ServletInitialHandler.java:99) [undertow-servlet-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.server.Connectors.executeRootHandler(Connectors.java:387) [undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at io.undertow.server.HttpServerExchange$1.run(HttpServerExchange.java:841) [undertow-core-2.2.5.Final-redhat-00001.jar!/:2.2.5.Final-redhat-00001]
	at org.jboss.threads.ContextClassLoaderSavingRunnable.run(ContextClassLoaderSavingRunnable.java:35) [jboss-threads-2.4.0.Final-redhat-00001.jar!/:2.4.0.Final-redhat-00001]
	at org.jboss.threads.EnhancedQueueExecutor.safeRun(EnhancedQueueExecutor.java:1990) [jboss-threads-2.4.0.Final-redhat-00001.jar!/:2.4.0.Final-redhat-00001]
	at org.jboss.threads.EnhancedQueueExecutor$ThreadBody.doRunTask(EnhancedQueueExecutor.java:1486) [jboss-threads-2.4.0.Final-redhat-00001.jar!/:2.4.0.Final-redhat-00001]
	at org.jboss.threads.EnhancedQueueExecutor$ThreadBody.run(EnhancedQueueExecutor.java:1377) [jboss-threads-2.4.0.Final-redhat-00001.jar!/:2.4.0.Final-redhat-00001]
	at org.xnio.XnioWorker$WorkerThreadFactory$1$1.run(XnioWorker.java:1280) [xnio-api-3.8.4.Final-redhat-00001.jar!/:3.8.4.Final-redhat-00001]
	at java.lang.Thread.run(Thread.java:748) [?:1.8.0_312]
```
This is due to a fundamental way in which Wildfly and JBoss loads classes where each java module 
needs to have its dependencies explicitly configures via XML files. Refer to 
[Wildfly Docs](https://docs.jboss.org/author/display/WFLY10/Class%20Loading%20in%20WildFly.html) for more details.
Another approach is to set the `jboss.modules.system.pkgs` system property where you can explicitly list package 
names to be supported in a comma-seperated list. This will make that list of packages visible to all modules.
For example, you would set the system property as: 
`-Djboss.modules.system.pkgs=java.util.logging,javax.management` and the packages `java.util.logging` and `javax.management` are now visible to all modules.
Thus, you would not get the exception `java.lang.NoClassDefFoundError` in our weaved class for those specific packages.  

In the Java Agent, we programmatically append certain classes to the system property `jboss.modules.system.pkgs` within the context of our premain method.
This provides visibility to the packages `java.util.logging`, `javax.management`, `java.lang.management`, and more to our Java Agent. 
If you are a developer and you encounter a `java.lang.NoClassDefFoundError` exception from a package used inside our weaved class, 
please refer to the `newrelic-agent` module, and inside the package `com.newrelic.agent.config` look for the class `JbossUtils`.
There you can update the list of classes that should be visible within all modules in JBoss EAP and Wildfly.

