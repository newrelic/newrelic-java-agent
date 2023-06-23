## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/aws-wrap-0.7.0/lib/aws-wrap_2.10-0.9.2.jar
```

## Required jar versions 
`aws-wrap_2.10` - 0.9.0 or above
