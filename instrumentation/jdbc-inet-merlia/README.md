## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/jdbc-inet-merlia/lib/merlia-7.03.jar
```

## Required jar versions 
`merlia` - 7.03 or above
