# HttpURLConnection instrumentation

This is a very old HTTP client and the APIs can be called in many combinations which can lead to unpredictable scenarios when trying to instrument it.

There is also the added complication that once the `connect` method has been called, whether explicitly or not, the `HttpURLConnection` headers become
immutable, and we can no longer add our distributed tracing headers.

Furthermore, legacy CAT (Cross Application Tracing) can cause additional problems if `reportAsExternal` is called too early which causes exceptions.

Because of these things, we need to keep some state on the order that `HttpURLConnection` APIs are invoked and in some cases we need to defer making a decision
on whether an external call should be recorded or not until we have a more complete picture on what APIs were called and in what order.

## Usage scenarios/implementation

This instrumentation creates a segment to do timing of external calls, starting when the first `HttpURLConnection` API is called and lasting until the external
call is recorded. Because of this the timing isn't as exact as tracing the individual methods. The reason for doing the timing in this manner is that it allows
the instrumentation to call `addOutboundRequestHeaders` and `reportAsExternal` on the same segment regardless of the order that the `HttpURLConnection` APIs are called in.

Calling `addOutboundRequestHeaders` will result in a span (which could be generated from a segment or a tracer) that links one APM entity to another APM entity
in a distributed trace, whereas calling `reportAsExternal` will result in a span being marked as "External HTTP" that links one APM entity to any host (whether
it is monitored by New Relic, or not). Calling both methods on a single segment instance ensures that both scenarios are associated with a single span in a
distributed trace. This mitigates a bug in previous versions of this instrumentation that was using tracers instead of a segment. The bug was that a tracer
associated with one method (typically `connect`) would call `addOutboundRequestHeaders` and a tracer for another method would call `reportAsExternal`, resulting
in two different spans in a single distributed trace showing the same external call being made twice, one linked to an APM entity and the other linked to an external host.

Much of this complication is due to the fact that once the `connect` method has been called, the `HttpURLConnection` header map can no longer be updated and any
calls to `addOutboundRequestHeaders` will fail. This means that if `connect` is explicitly called first we have no choice but to call `addOutboundRequestHeaders`
at that point and try to predict what sequence of events happens next and whether an external call should be recorded and, if so, how to call `reportAsExternal`
on the same tracer/segment so that only a single external span gets generated.

Behavior expected when `HttpURLConnection` APIs are called in different combos and orders.
* If only `connect` is called, then NO request is made over the wire and NO external call is reported. The instrumentation starts a `SegmentCleanupTask` if`connect` is called first, waits for a set period of time to determine if any further `HttpURLConnection` APIs are called before deciding how to proceed. If no other API is called, then the segment is just ignored and no external is reported. If any other method is called an external call will be recorded. 
* If `getOutputStream` is called alone or in combination with `connect`, then an external will only be recorded with DT (not CAT due to it causing exceptions when it reads from the input stream too early). When using CAT a `SegmentCleanupTask` will be started to determine if any methods were called after `getOutputStream` that would warrant reporting an external, or if the segment should be ignored.
* Calling any of `getInputStream`, `getResponseCode`, or `getHeaderFields` all result in an external call being recorded.

## SegmentCleanupTask and ScheduledThreadPoolExecutor tuning

For each external request where `connect` (or `getOutputStream` combined with CAT) is called first a `SegmentCleanupTask` `Runnable` gets scheduled to execute on a `ScheduledThreadPoolExecutor`. The
`SegmentCleanupTask` waits for a configurable period of time (default is 5 seconds) to determine if any other `HttpURLConnection` APIs are called that result in
a network request being issued. In most cases, another method will be called within this time period which will result in the scheduled task being cancelled and
removed from the executor's scheduled queue. In cases where no method is called that issues a network request, the `SegmentCleanupTask` `Runnable` will execute
and mark the Segment started when connect was called as ignored so that it doesn't appear as an external call in the related distributed trace.

The `ScheduledThreadPoolExecutor` is shared amongst all `HttpURLConnection` instances making external requests and the size of its managed threadpool can be
configured via `thread_pool_size`. The delay after which each `SegmentCleanupTask` queued up with the `ScheduledThreadPoolExecutor` will be executed can be
configured with `delay_ms`.

Config examples:

Yaml
```yaml
  class_transformer:
    com.newrelic.instrumentation.httpurlconnection:
      enabled: true
      segment_cleanup_task:
        thread_pool_size: 5
        delay_ms: 5000
```

System property
```
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.httpurlconnection.segment_cleanup_task.thread_pool_size=5
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.httpurlconnection.segment_cleanup_task.delay_ms=5000
```

Environment variable
```
NEW_RELIC_CLASS_TRANSFORMER_COM_NEWRELIC_INSTRUMENTATION_HTTPURLCONNECTION_SEGMENT_CLEANUP_TASK_THREAD_POOL_SIZE=5
NEW_RELIC_CLASS_TRANSFORMER_COM_NEWRELIC_INSTRUMENTATION_HTTPURLCONNECTION_SEGMENT_CLEANUP_TASK_DELAY_MS=5000
```

## Troubleshooting

Whenever a `SegmentCleanupTask` runs and marks the external `Segment` as ignored it means that no external call will be recorded for the `Segment`. 
When this happens it will generate a supportability metric named `Supportability/HttpURLConnection/SegmentIgnore/connect`. This means that either one of two
things:
1. There is a scenario where `connect` is called and never followed by any other `HttpURLConnection` APIs that issue a network request. In this case, it is correct that the `Segment` was ignored and no external call was recorded.
2. There is a scenario where `connect` is called and any other `HttpURLConnection` APIs that issue a network request did not occur before the `SegmentCleanupTask` ran. In this case, you can increase the `delay_ms` so that the `SegmentCleanupTask` is delayed long enough for the external call to successfully take place. 
3. There is a scenario where CAT is enabled and `getOutputStream` is called and never followed by any other `HttpURLConnection` APIs that issue a network request. In this case, it is correct that the `Segment` was ignored and no external call was recorded.

At `FINEST` level agent logs the number of queued tasks in the executor at the time of each external request will be logged. You can find these logs by
searching for the string `HttpURLConnection - number of queued cleanup tasks`. This can be useful to see if there are too many runnable tasks queued up at once
that could lead to memory pressure. Setting the `delay_ms` to a lower value will have the biggest impact on lowering the number of tasks that can be queued at
a given time, while increasing `thread_pool_size` can help remove queued tasks quicker.

Threads in the threadpool managed by the executor will be named `New Relic HttpURLConnection Segment Cleanup Task`.

Another issue to be aware of is that legacy CAT (Cross Application Tracing) can cause additional problems if `reportAsExternal` is called too early. As seen in the stacktrace below
it tries to process the inbound response headers which will trigger a call to `getHeaderField` on the `HttpURLConnection` instance which
forces it to connect and read from the input stream. Unfortunately, for users of `HttpURLConnection` this has the unexpected effect of rendering the
`HttpURLConnection` header map immutable as well as causing `ProtocolException: Cannot write output after reading input` and
`IOException: Stream is closed` exceptions when attempting to write to the output stream. Because the headers become immutable some customers saw the `Content-Length` header disappear from certain outgoing HTTP requests from their applications, which resulted in external servers rejecting requests with HTTP `411 Length Required` errors.

```java
"java.base/sun.net.www.protocol.http.HttpURLConnection$StreamingOutputStream.close(HttpURLConnection.java:3834)"
"java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1622)"
"java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1589)"
"java.base/sun.net.www.protocol.http.HttpURLConnection.getHeaderField(HttpURLConnection.java:3221)"
"com.nr.agent.instrumentation.httpurlconnection.InboundWrapper.getHeader(InboundWrapper.java:26)"
"com.newrelic.agent.HeadersUtil.getAppDataHeader(HeadersUtil.java:122)"
"com.newrelic.agent.CrossProcessTransactionStateImpl.processInboundResponseHeaders(CrossProcessTransactionStateImpl.java:330)"
"com.newrelic.agent.tracers.DefaultTracer.recordInboundResponseHeaders(DefaultTracer.java:685)"
"com.newrelic.agent.tracers.DefaultTracer.recordExternalMetricsHttp(DefaultTracer.java:722)"
"com.newrelic.agent.tracers.DefaultTracer.recordExternalMetrics(DefaultTracer.java:664)"
"com.newrelic.agent.tracers.DefaultTracer.recordMetrics(DefaultTracer.java:469)"
"com.newrelic.agent.tracers.DefaultTracer.performFinishWork(DefaultTracer.java:284)"
"com.newrelic.agent.tracers.DefaultTracer.finish(DefaultTracer.java:236)"
"com.newrelic.agent.Transaction.finishTracer(Transaction.java:2370)"
"com.newrelic.agent.Transaction.finishSegment(Transaction.java:2365)"
"com.newrelic.agent.Segment$1.run(Segment.java:202)"
"com.newrelic.agent.ExpirationService.expireSegmentInline(ExpirationService.java:47)"
"com.newrelic.agent.Segment.finish(Segment.java:214)"
"com.newrelic.agent.Segment.end(Segment.java:144)"
"com.nr.agent.instrumentation.httpurlconnection.MetricState.reportExternalCall(MetricState.java:255)"
```

## Example HttpURLConnection usage

The example Spring controller code below shows common use cases of `HttpURLConnection`.  
* Calling `getInputStream` to make `GET` requests
* Calling `getOutputStream` to make `POST` requests

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
public class ExternalController {
    private static String URL = "https://example.com/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalController.class);

    /**
     * Make a GET request using HttpURLConnection
     */
    @GetMapping("/httpurlconnection/get")
    public String get() throws IOException, URISyntaxException {
        URI uri = new URI(URL);
        URL url = uri.toURL();

        // openConnection does no network IO, it simply creates an HttpURLConnection instance
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // prepare the GET request
        con.setRequestMethod("GET");

        try {
            // connect can be called explicitly, but it doesn't need to be as it will implicitly be called by other
            // methods that read or write over the connection such as getInputStream, getResponseCode, and getOutputStream.
            // Calling connect directly doesn't cause a request to happen over the wire.
            con.connect();

            // getInputStream opens a stream with the intention of reading response data from the server.
            // Calling getInputStream causes a request to happen over the wire.
            StringBuilder stringBuilder = getInputStreamString(con);

            // getResponseCode gets the status code from an HTTP response message.
            // If the request was already made (e.g. because getInputStream was called before it) it will simply return the status code from the response.
            // Otherwise, it will initiate the request itself by calling getInputStream which calls connect (or potentially getOutputStream if streaming).
            int responseCode = con.getResponseCode();
        } catch (IOException e) {
            LOGGER.info("Caught exception: " + e);
        } finally {
            // close and release the connection resource
            con.disconnect();
        }

        return "something useful";
    }

    /**
     * Make a POST request using HttpURLConnection
     */
    @PostMapping("/httpurlconnection/post")
    public String post(String foo) throws IOException, URISyntaxException {
        URL = URL + "?foo=" + foo;
        URI uri = new URI(URL);
        URL url = uri.toURL();

        // openConnection does no network IO, it simply creates an HttpURLConnection instance
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // prepare the POST request
        con.setDoOutput(true);
        con.setChunkedStreamingMode(0);

        try {
            // connect can be called explicitly, but it doesn't need to be as it will implicitly be called by other
            // methods that read or write over the connection such as getInputStream, getResponseCode, and getOutputStream.
            // Calling connect directly doesn't cause a request to happen over the wire.
            con.connect();

            // getOutputStream opens a stream with the intention of writing data to the server but doesn't actually write until you call writer.write.
            // Calling getOutputStream causes a request to happen over the wire regardless if data was written or not.
            // It can also be called in a fire and forget manner without ever inspecting the response.
            OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
            String message = URLEncoder.encode(foo, StandardCharsets.UTF_8);
            // actually write data over the wire
            writer.write("message=" + message);
            writer.close();

            // getInputStream opens a stream with the intention of reading response data from the server.
            // Calling getInputStream causes a request to happen over the wire.
            StringBuilder stringBuilder = getInputStreamString(con);

            // getResponseCode gets the status code from an HTTP response message.
            // If the request was already made (e.g. because getInputStream was called before it) it will simply return the status code from the response.
            // Otherwise, it will initiate the request itself by calling getInputStream which calls connect (or potentially getOutputStream if streaming).
            int responseCode = con.getResponseCode();

            // getHeaderFields returns an unmodifiable Map of the header fields.
            // Calling getHeaderFields causes a request to happen over the wire as it directly calls getInputStream.
            Map<String, List<String>> headerFields = con.getHeaderFields();
        } catch (IOException e) {
            LOGGER.info("Caught exception: " + e);
        } finally {
            // close and release the connection resource
            con.disconnect();
        }

        return "something useful";
    }

    private static StringBuilder getInputStreamString(HttpURLConnection con) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder;
    }
}
```
