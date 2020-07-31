## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/websphere-liberty-profile-dispatcher-8.5.5.5/lib/com.ibm.ws.kernel.boot_1.0.6.jar
```

## Required jar versions 
`com.ibm.ws.kernel.boot` - 1.0.6 or above
