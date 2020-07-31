## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/jdbc-ojdbc/lib/ojdbc7-12.1.0.1.jar
```

## Required jar versions 
`ojdbc7` - 12.1.0.1.0 or above
