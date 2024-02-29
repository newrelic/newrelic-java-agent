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
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework" })
@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
public class Spring600RouterFuncTest {
    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static WebClient webClient;

    @BeforeClass
    public static void setup() throws Exception {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");

        final int port = TestSocketUtils.findAvailableTcpPort();
        final String host = "localhost";

        final HttpHandler httpHandler = SpringTestHandler.httpHandler(server.getEndPoint());

        webClient = WebClient.builder().baseUrl(String.format("http://%s:%d", host, port))
                .clientConnector(new ReactorClientHttpConnector()).build();

        HttpServer.create()
                .host("localhost")
                .port(port)
                .handle(new ReactorHttpHandlerAdapter(new HttpHandler() {
                    @Override
                    @Trace(dispatcher = true)
                    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
                        return httpHandler.handle(request, response);
                    }
                }))
                .bind()
                .block();
    }

    @Test
    public void simplePath() {
        webClient.get().uri("/").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/ (GET)"));
    }

    @Test
    public void helloWorldPath() {
        webClient.get().uri("/helloWorld").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/helloWorld (GET)"));
    }

    @Test
    public void webClientPath() {
        webClient.get().uri("/web-client").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/web-client (GET)"));
    }

    @Test
    public void noMatch() {
        expectedException.expect(WebClientResponseException.class);
        int statusCode = webClient.post()
                .uri("/createUser")
                .contentType(MediaType.TEXT_PLAIN)
                .retrieve()
                .toBodilessEntity()
                .block()
                .getStatusCodeValue();
        assertEquals(404, statusCode);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/Unknown Route (POST)"));
    }

    @Test
    public void createPersonPath() {
        webClient.post()
                .uri("/person")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity()
                .block()
                .getStatusCodeValue();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames()
                .contains("OtherTransaction/Spring/person (POST)"));
    }

    @Test
    public void nested() {
        assertEquals(200, webClient.get().uri("/language/en-us/nested").exchange().block().statusCode().value());
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames()
                .contains("OtherTransaction/Spring/language/{language}/nested (GET)"));
    }

    @Test
    public void postRegex() {
        final String responseBody = webClient.post().uri("/path/ToNowhere!!!!")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"this\": \"isJSON\"}")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("Got[where] = " + "ToNowhere!!!!", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/path/{where} (POST)"));
    }

    @Test
    public void queryParam() {
        final String responseBody = webClient.get().uri("/wat/wat/wat?bar=java")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("query parameter request", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/QueryParameter/bar (GET)"));
    }

    @Test
    public void headers() {
        final String responseBody = webClient.get()
                .uri("/some/other/path")
                .header("SpecialHeader", "productive")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        assertEquals("Headers request", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/Unknown Route (GET)"));
    }

    @Test
    public void pathExtension() {
        final String responseBody = webClient.get()
                .uri("favorite.html")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        assertEquals("Here's your html file", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/PathExtension/html (GET)"));
    }

    @Test
    public void contentType() {
        final int statusCode = webClient.post()
                .uri("/uploadPDF")
                .contentType(MediaType.APPLICATION_PDF)
                .retrieve()
                .toBodilessEntity()
                .block()
                .getStatusCodeValue();
        assertEquals(200, statusCode);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames()
                .contains("OtherTransaction/Spring/uploadPDF (POST)"));
    }

    @Test
    public void resources() {
        final String response = webClient.get()
                .uri("files/numbers.txt")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", response);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/files/{*} (GET)"));
    }

    @Test
    public void pathAccept() {
        final String response = webClient.get()
                .uri("/path-to-greatness")
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("Path to greatness", response);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/path-to-greatness (GET)"));
    }

    private String msg(Introspector introspector) {
        return "Couldn't find transaction name, but found other transactions: " + introspector.getTransactionNames();
    }
}
