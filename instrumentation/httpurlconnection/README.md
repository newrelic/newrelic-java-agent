# HttpURLConnection instrumentation

This is a very old HTTP client and the APIs can be called in many combinations which can lead to unpredictable scenarios when trying to instrument it.

There is also the added complication that once the `connect` method has been called, whether explicitly or not, the `HttpURLConnection` headers become immutable, and we can no longer add our distributed tracing headers.

Because of these things, we need to keep some state on the order that `HttpURLConnection` APIs are invoked and in some cases we need to defer making a decision on whether an external call should be recorded or not until we have a more complete picture on what APIs were called and in what order.

## Usage scenarios/implementation

This instrumentation creates a segment to do timing of external calls, starting when the first `HttpURLConnection` API is called and lasting until the final method completes and the external call is recorded. Because of this the timing isn't as exact as tracing the individual methods. The reason for doing the timing in this manner is that it allows the instrumentation to call `addOutboundRequestHeaders` and `reportAsExternal` on the same segment regardless of the order that the `HttpURLConnection` APIs are called in.

Calling `addOutboundRequestHeaders` will result in a span (which could be generated from a segment or a tracer) that links one APM entity to another APM entity in a distributed trace, whereas calling `reportAsExternal` will result in a span being marked as "External HTTP" that links one APM entity to any host (whether it is monitored by New Relic, or not). Calling both methods on a single segment instance ensures that both scenarios are associated with a single span in a distributed trace. This mitigates a bug in previous versions of this instrumentation that was using tracers instead of a segment. The bug was that a tracer associated with one method (typically `connect`) would call `addOutboundRequestHeaders` and a tracer for another method would call `reportAsExternal`, resulting in two different spans in a single distributed trace showing the same external call being made twice, one linked to an APM entity and the other linked to the hostname.

Much of this complication is due to the fact that once the `connect` method has been called, the `HttpURLConnection` header map can no longer be updated and any calls to `addOutboundRequestHeaders` will fail. This means that if `connect` is explicitly called first we have no choice but to call `addOutboundRequestHeaders` at that point and try to predict what sequence of events happens next and whether an external call should be recorded and, if so, how to call `reportAsExternal` on the same tracer/segment so that only a single external span gets generated.

Behavior expected when `HttpURLConnection` APIs are called in different combos and orders.
* If only `connect` is called, then NO request is made over the wire and NO external call is reported. The instrumentation starts a `TimerTask` if `connect` is called first, waits for a set period of time to determine if any further `HttpURLConnection` APIs are called before deciding how to proceed. If no other API is called, then the segment is just ignored and no external is reported. If `getOutputStream` was called, a similar process takes place to determine when to record an external call. If any other method is called an external call will be recorded when getting the response.
* If only `getOutputStream` is called, then a request is made over the wire and an external call is reported, regardless if any data was actually written to the output stream. However, this can be done in a fire & forget manner without ever inspecting the response. Like, described above, a `TimerTask` is started that waits for a set period of time to determine if any further `HttpURLConnection` APIs are called before deciding how to proceed. If no other API is called, then the segment is ended when the `TimerTask` ends and an external is reported with that timing.
* If any combination/ordering of `connect` and `getOutputStream` are called, then the behavior is exactly the same as only calling `getOutputStream` as described above.
* Calling any of `getInputStream`, `getResponseCode`, or `getHeaderFields` all result in the response being inspected and an external call being recorded.

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
