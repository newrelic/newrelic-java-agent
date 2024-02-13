/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework" })
@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
public class SpringRouterEdgeCaseTest {

    private static WebClient webClient;

    @BeforeClass
    public static void setup() {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");

        final String host = "localhost";
        final int port = TestSocketUtils.findAvailableTcpPort();
        final HttpHandler httpHandler = SpringTestHandler.httpHandlerNested();

        webClient = WebClient.builder().baseUrl(String.format("http://%s:%d", host, port))
                .clientConnector(new ReactorClientHttpConnector()).build();

        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

        HttpServer.create()
                .host("localhost")
                .port(port)
                .handle(new BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>() {
                    @Override
                    @Trace(dispatcher = true)
                    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
                        return adapter.apply(httpServerRequest, httpServerResponse);
                    }
                })
                .bind()
                .block();
    }

    @Test
    public void personPath() {
        final String response = webClient.get().uri("/person/").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/person (GET)"));
    }

    @Test
    public void badResponseCode() {
        final int statusCode = webClient.get().uri("/bad-status-code").exchange().block().rawStatusCode();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertEquals(463, statusCode);
    }

}
