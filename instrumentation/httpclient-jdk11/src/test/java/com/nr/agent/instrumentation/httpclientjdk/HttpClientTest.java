package com.nr.agent.instrumentation.httpclientjdk;

import com.newrelic.agent.introspec.*;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.nr.agent.instrumentation.httpclient", "nr.weave.java.net.http", "jdk.internal.net.http"})
public class HttpClientTest {

    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

    static URI endpoint;
    static String host;

    @BeforeClass
    public static void before() {
        try {
            endpoint = server.getEndPoint();
            host = endpoint.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() throws IOException, URISyntaxException, InterruptedException {
        httpClientSendExternal(host, false);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount());
        final String txTwo = introspector.getTransactionNames().iterator().next();
        // creates a scoped (and unscoped)
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txTwo, "External/UnknownHost/HttpAsyncClient/failed"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/HttpAsyncClient/failed"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }


    /**
     * Start a background transaction, make an external request using httpclient, then finish.
     */
    @Trace(dispatcher = true)
    private void httpClientSendExternal(String host, boolean doCat) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(host))
                .setHeader(HttpTestServer.DO_CAT, String.valueOf(doCat))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder().build().send(request,
                HttpResponse.BodyHandlers.ofString());

    }
}
