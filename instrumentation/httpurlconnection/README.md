# HttpURLConnection instrumentation

This is a very old HTTP client and the APIs can be called in many combinations which can lead to unpredictable scenarios when trying to instrument it.

There is also the added complication that once the `connect` method has been called, whether explicitly or not, the `HttpURLConnection` headers become immutable, and we can no longer add our distributed tracing headers.

This causes a problem where the DT headers have to be added in the `connect` tracer, but the real external call happens when one of `getResponseCode`, `getResponseMessage` or `getInputStream` is called.

## History

The instrumentation in version 7.11 would add the DT headers in the first method that was called but add the external to the method that read the response. This caused troubles in DT page, where it would appear that 2 calls were made, one to the DT entity and another to the external.

The instrumentation in version 8.0 added a segment that would hold both the DT headers and be marked as the external. This solved the problem with the previous instrumentation, but caused a problem with the timing of the request. Segments are used for async work, so now it looked like there was async work in a single threaded part of the application. And since it is async, its time was not removed from the parent tracer, making the time of the segment to be double counted in the request. But the total request time was correct.

## New solution

The current solution resembles the 7.11 instrumentation, but after the external is reported, the guid of the DT tracer and the external tracer are swapped. This causes the downstream DT to be linked to the external tracer. This is a dangerous if done carelessly, but in this case, both tracers are leaf tracers, so there are no tracers that depend on the guids of these tracers.

### Usage scenarios/implementation

The instrumentation cares mostly about the following methods: `connect`, `getOutputStream`, `getResponseCode`, `getResponseMessage` and `getInputStream`. These are the methods that change the state of the HttpURLConnection and affect the instrumentation. Henceforth, _response methods_ can refer to any of `getResponseCode`, `getResponseMessage` and `getInputStream`.

#### Scenario 1 (connect + response)

When `connect` or `getOutputStream` is the first method called, DT headers are added to the request, linked to the method's tracer (DT tracer). In any subsequent call to these methods, the instrumentation code is noop.

Then in a subsequent call to a response method, the method's tracer will be marked as the external (external tracer) and the guids from the external tracer, and the DT tracer are swapped. In subsequent calls for any methods the instrumentation code is noop.

Depending on the configuration of the HttpURLConnection, the external call can start after the call to `getOutpuStram` is finished and before any response method is called. In this case, the external metric will (most likely) record a duration lower than the real duration of the outgoing request.

#### Scenario 2 (response)

When a response method is the first method called, then the tracer will be marked as both the DT tracer and the external tracer. No guid swapping needed in this case.

In further calls to any method, the instrumentation code is noop.

#### Scenario 3 (fire and forget)

When `getOutputStream` is called (with or without a preceding `connect` call), but none of the response methods is called after, could result in a fire and forget call.

As in scenario 1, the first method called will have the DT headers associated with it. But since there is no external tracer, there is no guids to swap.

In this case, there will be no external reported, but if the call reaches a server that is instrumented, then the DT will be linked to the first method call.

Not setting an external in this case is reasonable because there is no way for the agent to report how long the call really took. The call itself may start after the transaction has ended and will usually be longer than the time taken by the `connect` or `getOutputStream` call.

### Flaws

- Fire and forget are not computed into the metrics.
- The timing of the external may be shorter than it really is when `getOutputStream` is called under certain circumstances.

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
