package com.nr.ratpack.http.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import org.junit.runner.RunWith;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Headers;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ratpack" })
public class HttpClientTest {

    @Test
    public void externalRequest() throws Exception {

        try (EmbeddedApp echoApp = echoApp();
             EmbeddedApp requestApp = requestApp(echoApp.getAddress())) {

            // Invoke requestApp handler, which in turn calls echo App
            ReceivedResponse response = requestApp.getHttpClient().get("/");
            assertEquals("Everything is alright", response.getBody().getText());

            Introspector introspector = InstrumentationTestRunner.getIntrospector();

            int finishedTransactionCount = introspector.getFinishedTransactionCount(10000);
            Collection<String> transactionNames = introspector.getTransactionNames();
            if (finishedTransactionCount > 1) {
                for (String transactionName: transactionNames) {
                    System.out.println("Found transaction: " + transactionName);
                }
            }
            assertEquals(1, finishedTransactionCount);
            String expectedTxnName = "OtherTransaction/Custom/com.nr.ratpack.http.instrumentation.HttpClientTest$1/handle";
            assertTrue(transactionNames.contains(expectedTxnName));

            Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTxnName);
            assertTrue(metrics.containsKey("External/" + echoApp.getAddress().getHost() + "/RatpackHttpClient/success"));
        }
    }

    EmbeddedApp echoApp() {
        return EmbeddedApp.fromHandler(ctx -> ctx.getRequest().getBody().then(body -> ctx.render(body.getText())));
    }

    EmbeddedApp requestApp(URI uri) {
        return EmbeddedApp.fromHandler(new Handler() {
            @Override
            @Trace(dispatcher = true)
            public void handle(Context ctx) throws Exception {

                // Make an async HTTP to URI
                ctx.get(HttpClient.class).get(uri).onError(error -> {
                    ctx.error(error);
                }).then(response -> {
                    ctx.render("Everything is alright");
                });
            }
        });
    }

    @Test
    public void externalRequestStreamed() throws Exception {

        try (EmbeddedApp echoApp = echoApp();
             EmbeddedApp requestApp = requestStreamedApp(echoApp.getAddress())) {

            // Invoke requestApp handler, which in turn calls echo App
            ReceivedResponse response = requestApp.getHttpClient().get("/");
            assertEquals("Everything is alright", response.getBody().getText());

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            assertEquals(1, introspector.getFinishedTransactionCount(10000));

            Collection<String> transactionNames = introspector.getTransactionNames();
            String expectedTxnName = "OtherTransaction/Custom/com.nr.ratpack.http.instrumentation.HttpClientTest$2/handle";
            assertTrue(transactionNames.contains(expectedTxnName));

            Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTxnName);
            assertTrue(metrics.containsKey("External/" + echoApp.getAddress().getHost() + "/RatpackHttpClient/success"));
        }
    }

    EmbeddedApp requestStreamedApp(URI uri) {
        return EmbeddedApp.fromHandler(new Handler() {
            @Override
            @Trace(dispatcher = true)
            public void handle(Context ctx) throws Exception {

                // Make an async HTTP to URI
                ctx.get(HttpClient.class).requestStream(uri, requestSpec -> {}).onError(error -> {
                    ctx.error(error);
                }).then(response -> {
                    ctx.render("Everything is alright");
                });
            }
        });
    }

    @Test
    public void testOutboundHeaders() throws Exception {
        /*
        This test spins up two Ratpack servers (ephemeral ports)

        Sequence diagram:

        Test -- blocking request to --->  Second Server

                                          Second Server --- async request to ---> First Server

                                          Second Server <--- responds to  ------- First Server

        Test <---- responds to --------   Second Server

        */

        final AtomicBoolean nrHeadersSeen = new AtomicBoolean(false);

        try (EmbeddedApp firstApp = EmbeddedApp.fromHandler(new Handler() {
            @Override
            @Trace(dispatcher = true)
            public void handle(Context ctx) throws Exception {
                Headers headers = ctx.getRequest().getHeaders();
                if (headers.contains("X-NewRelic-Transaction") &&
                    headers.contains("X-NewRelic-ID")) {
                   nrHeadersSeen.set(true);
                }

                ctx.render("success");
            }
        });

         EmbeddedApp secondApp = EmbeddedApp.fromHandler(new Handler() {
            @Override
            @Trace(dispatcher = true)
            public void handle(Context ctx) throws Exception {
                URI firstAppAddress = firstApp.getAddress();

                ctx.get(HttpClient.class)
                   .get(firstAppAddress)
                   .onError(error -> {
                      ctx.render("First app should've responded. Algo malo pasó.");
                   })
                   .then(response -> {
                       ctx.render(response.getBody().getText());
                      }
                   );
            }
        })) {

            ReceivedResponse response = secondApp.getHttpClient().get("/");
            assertEquals("success", response.getBody().getText());
            assertTrue(nrHeadersSeen.get());
        }
    }

    @Test
    public void testRedirectOutboundHeaders() {
         final AtomicBoolean nrHeadersSeen = new AtomicBoolean(false);

        try (EmbeddedApp echoApp = EmbeddedApp.fromHandler(new Handler() {
            @Override
            @Trace(dispatcher = true)
            public void handle(Context ctx) throws Exception {
                Headers headers = ctx.getRequest().getHeaders();
                if (headers.contains("X-NewRelic-Transaction") &&
                    headers.contains("X-NewRelic-ID")) {
                   nrHeadersSeen.set(true);
                }

                ctx.render("success");
            }
        });

        EmbeddedApp redirectApp = EmbeddedApp.fromHandler(ctx -> ctx.redirect(echoApp.getAddress()));

        EmbeddedApp endpoint = EmbeddedApp.fromHandler(new Handler() {

            @Override
            @Trace(dispatcher = true)
            public void handle(Context ctx) throws Exception {
                ctx.get(HttpClient.class)
                   .get(redirectApp.getAddress())
                   .onError(error -> {
                      ctx.render("First app should've responded.");
                   })
                   .then(response -> {
                       ctx.render(response.getBody().getText());
                      }
                   );
            }
        })) {
            ReceivedResponse response = endpoint.getHttpClient().get("/");
            assertEquals("success", response.getBody().getText());
            assertTrue(nrHeadersSeen.get());
        }

    }



}
