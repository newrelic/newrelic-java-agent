# JBoss Logging

JBoss Logging builds on top of JUL (`java.util.logging`) and shares some functionality with the JUL instrumentation.

This instrumentation weaves `org.jboss.logmanager.Logger` (which extends `java.util.logging.Logger`) 
to generate logging metrics and forward log events. However, this instrumentation does not do local log
decorating as that functionality is already handled by the JUL instrumentation module.  

