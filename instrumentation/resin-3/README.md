## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/resin-3/lib/resin-3.jar
instrumentation/resin-3/lib/resin-util-3.jar
```

## Required jar versions 
`resin` - 3.1.x up to, but not including, 4.0.0  
`resin-util` - 3.1.x up to, but not including, 4.0.0
