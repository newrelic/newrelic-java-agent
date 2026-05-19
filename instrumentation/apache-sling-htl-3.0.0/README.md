# apache-sling-htl-3.0.0

This module is for injecting the browser monitoring header script into Apache [Sling](https://sling.apache.org/) 
[HTL](https://sling.apache.org/documentation/bundles/scripting/scripting-htl.html) templates. HTL is the engine utilized by Adobe Experience Manager (AEM).

Note that this module targets Apache Sling 3.0.0 through current. This version range targets the
`jakarta` namespace.

### Module Application
The module instruments the base interface of `jakarta.servlet.ServletResponse`. In order to prevent this from
applying to almost every Java application in existence, the module also does an empty weave of the 
`org.apache.sling.api.SlingConstants` class (`Require_SlingConstants`) to only target Sling based apps.

### Injection Flow
The injection flow is as follows:
- Intercept calls to the `getWriter` and/or `getOutputStream` methods for implementers of `ServletResponse`.
- If `browser_monitoring.auto_instrument` is `true` and the mime type of the existing response is `text/html`, 
wrap the writer or output stream in a `NewRelicPrintWriterWrapper` or `NewRelicOutputStreamWrapper`. 
- The wrapper classes perform a "passthrough search" type algorithm to check for the existence of the 
`<head>` or `<HEAD>` tag. If found, the browser script is injected immediately after the tag.
- Once found, a flag is set so that the tag search is disabled and the wrapper simply acts as a pass though
to the underlying writer or output stream.

## Additional Checks
- The instrumented methods check to make sure that the writer/output stream returned by the `Weaver.callOriginal`
call isn't already a `NewRelicXXXXXXWrapper` instance, by doing a check on the class name. An `instanceof`
check doesn't work because of the way OSGI class loading works.
- Because we're modifying the size of the response, we also intercept the `setContentLength` and
`setContentLengthLong` methods and basically ignore them to force the response type to chunked; otherwise
the response will be truncated by the browser.

### Other Nonsense
Because of the way Sling chains response writers, it's possible that we will wrap multiple `ServletResponse`
instances, each checking for the `head` tag. There's not really anything that can be done for this, other than
introducing a thread local flag solution, but that seems like overkill for something that doesn't actually
affect the injection since the script will only be injected one time.

This module is only responsible for injection of the browser script. Other instrumentation (custom, labs)
will need to be present to properly name the transaction and capture deeper visibility in Sling/AEM
transactions.

