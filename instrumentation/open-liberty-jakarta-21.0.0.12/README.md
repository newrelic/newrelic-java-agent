## Building

New Relic does not distribute the jar(s) required to build this instrumentation module nor are they available from a public repository such as Maven Central or jcenter.

To build this instrumentation module you must provide the jar(s) and place them into the `/lib` subdirectory as follows:

```groovy
instrumentation/open-liberty-21.0.0.12/lib/com.ibm.ws.webcontainer.jakarta_1.1.59.jar
```

## Required jar versions 

`com.ibm.ws.webcontainer.jakarta_1.1.59.jar` - or above

This jar can be found in the [Open Liberty Jakarta EE 9 releases](https://openliberty.io/start/#runtime_releases) for versions `21.0.0.12`+
* [openliberty-jakartaee9-21.0.0.12.zip](https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/2021-11-17_1256/openliberty-jakartaee9-21.0.0.12.zip)
* Expand the ZIP and search jars in `wlp/lib/`

## Dependency Usage

The required jar is used to compile the following classes:
* `com.ibm.ws.webcontainer.webapp.WebApp_Instrumentation`
* `com.nr.agent.instrumentation.websphere.NRServletRequestListener`