## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/weblogic-12/lib/weblogic-classes.jar
instrumentation/weblogic-12/lib/com.bea.core.weblogic.rmi.client_3.0.0.0.jar
instrumentation/weblogic-12/lib/com.bea.core.utils.full_2.2.0.0.jar
instrumentation/weblogic-12/lib/com.bea.core.weblogic.web.api_3.0.0.0.jar
```

## Required jar versions 
`weblogic-classes` - 12.1.2.1 up to, but not including, 12.2  
`com.bea.core.weblogic.rmi.client` - 3.0.0.0  
`com.bea.core.utils.full` - 2.2.0.0  
`com.bea.core.weblogic.web.api` - 3.0.0.0
