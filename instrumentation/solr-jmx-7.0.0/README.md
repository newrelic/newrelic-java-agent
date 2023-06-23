## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/solr-jmx-7.0.0/lib/org.restlet-2.3.0.jar
instrumentation/solr-jmx-7.0.0/lib/org.restlet.ext.servlet-2.3.0.jar
```

## Required jar versions 
`org.restlet` - 2.3.0 or above
`org.restlet.ext.servlet` - 2.3.0 or above