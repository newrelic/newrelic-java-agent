## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/jboss-7/lib/jbossweb-7.5.10.Final.jar
```

## Required jar versions 
`jbossweb` - 7.5.7.Final or above
