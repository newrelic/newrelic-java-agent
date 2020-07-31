## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/websphere-jmx-7/lib/com.ibm.ws.admin.core.jar
```

## Required jar versions 
`com.ibm.ws.admin.core` - 7.0.0 or above
