## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/weblogic-jmx-12.2.1/lib/com.oracle.weblogic.server.jar
```

## Required jar versions 
`com.oracle.weblogic.server` - 12.2.1.0 or above
