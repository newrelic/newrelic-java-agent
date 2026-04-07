# glassfish-jul-extension-7

Glassfish uses JUL as its default logger but the `GlassFishLogger` extends `java.util.logging.Logger` in a way that prevents the `java.logging-jdk8` instrumentation from applying.

Essentially, when using the `glassfish-jul-extension` logging library, the `java.logging-jdk8` instrumentation will work for local decorating but fail to apply for the log forwarding and log metrics functionality. This instrumentation module provides the missing functionality when using the `glassfish-jul-extension` logging library by weaving `org.glassfish.main.jul.GlassFishLogger` to forward logs and generate log metrics.
