## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/websphere-8/lib/com.ibm.ws.runtime-8.jar
instrumentation/websphere-8/lib/com.ibm.ws.webcontainer-8.jar
instrumentation/websphere-8/lib/was_public-8.jar
```

## Required jar versions 
`com.ibm.ws.runtime` - WebSphere Application Server 8 or 9  
`com.ibm.ws.webcontainer` - WebSphere Application Server 8 or 9   
`was_public` - WebSphere Application Server 8 or 9
