## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/weblogic-jmx-12/lib/weblogic-classes.jar
```

## Required jar versions 
`weblogic-classes` - 12.x.x.x up to, but not including, 12.2.1.0
