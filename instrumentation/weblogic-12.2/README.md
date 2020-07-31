## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/weblogic-12.2/lib/weblogic-classes.jar
instrumentation/weblogic-12.2/lib/com.bea.core.weblogic.rmi.client_3.0.0.0.jar
instrumentation/weblogic-12.2/lib/com.oracle.weblogic.server.channels.jar
```

## Required jar versions 
`weblogic-classes` - 12.2.x.x
`com.bea.core.weblogic.rmi.client` - 3.0.0.0
`com.oracle.weblogic.server.channels` - 12.2.x.x
