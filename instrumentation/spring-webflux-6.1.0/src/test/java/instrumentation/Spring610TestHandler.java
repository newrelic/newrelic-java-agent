/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package instrumentation;

import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import org.junit.experimental.categories.Category;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RequestPredicates.headers;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RequestPredicates.pathExtension;
import static org.springframework.web.reactive.function.server.RequestPredicates.queryParam;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RouterFunctions.toHttpHandler;

@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
class Spring610TestHandler {
    static HttpHandler httpHandler(URI endPoint) {
        final Spring610TestHandler handlers = new Spring610TestHandler();

        RouterFunction<ServerResponse> route = route(GET("/"), Spring610TestHandler::sample)
                .andRoute(GET("/helloWorld"), Spring610TestHandler::helloWorld)
                .andNest(GET("/language/{language}"), route(GET("/nested"), Spring610TestHandler::nested))
                .andRoute(POST("/createUser").and(contentType(APPLICATION_JSON)), Spring610TestHandler::createUser)
                .andRoute(POST("/path/{where}"), Spring610TestHandler::pathRequest)
                .andRoute(queryParam("bar", str -> str.endsWith("java")), Spring610TestHandler::queryParameter)
                .andRoute(headers(Spring610TestHandler::headerPredicate), Spring610TestHandler::headersHandler)
                .andRoute(contentType(MediaType.APPLICATION_PDF, MediaType.APPLICATION_XML).and(POST("/uploadPDF")), Spring610TestHandler::pdfHandler)
                .andRoute(POST("/person").and(accept(APPLICATION_JSON)).and(contentType(APPLICATION_JSON)), Spring610TestHandler::createUser)
                .andRoute(path("/path-to-greatness").and(accept(MediaType.TEXT_PLAIN)), Spring610TestHandler::pathToGreatness)
                .andRoute(pathExtension("html"), Spring610TestHandler::htmlHandler)
                .andRoute(path("/web-client"), makeWebClientHandler(endPoint));

        final RouterFunction<?> routerFunction = route.andOther(resources("/files/{*}", new ClassPathResource("files/")));

        return toHttpHandler(routerFunction);
    }

    private static Mono<ServerResponse> pathToGreatness(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Mono.just("Path to greatness"), String.class);
    }

    static HttpHandler httpHandlerNested() {
        final Spring610TestHandler handlers = new Spring610TestHandler();

        RouterFunction<ServerResponse> route = nest(path("/person"),
                nest(accept(APPLICATION_JSON), route(GET("/{id}"), handlers::getPerson).
                        andRoute(method(HttpMethod.GET), handlers::listPeople))
                        .andRoute(POST("/").and(contentType(APPLICATION_JSON)), handlers::createPerson))
                .andRoute(path("/bad-status-code"), Spring610TestHandler::badStatusCode);

        final RouterFunction<?> routerFunction = route.andOther(resources("/files/{*}", new ClassPathResource("files/")));

        return toHttpHandler(routerFunction);
    }

    private Mono<ServerResponse> createPerson(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Mono.just("person"), String.class);
    }

    private Mono<ServerResponse> listPeople(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Mono.just("person list"), String.class);
    }

    private Mono<ServerResponse> getPerson(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Mono.just("person"), String.class);
    }

    private static Mono<ServerResponse> sample(ServerRequest request) {
        return ServerResponse.ok().body(Mono.just("Sample"), String.class);
    }

    private static Mono<ServerResponse> helloWorld(ServerRequest req) {
        return ServerResponse.ok().body(Mono.just("Hello, world!"), String.class);
    }

    private static Mono<ServerResponse> nested(ServerRequest request) {
        return ServerResponse.ok().body(Mono.just("nested!"), String.class);
    }

    private static Mono<ServerResponse> createUser(ServerRequest request) {
        return ServerResponse.ok().body(Mono.just("User created"), String.class);
    }

    private static Mono<ServerResponse> pathRequest(ServerRequest req) {
        return ServerResponse.ok().body(Mono.just("Got[where] = " + req.pathVariable("where")), String.class);
    }

    private static Mono<ServerResponse> queryParameter(ServerRequest req) {
        return ServerResponse.ok().body(Mono.just("query parameter request"), String.class);
    }

    private static boolean headerPredicate(ServerRequest.Headers req) {
        return !req.header("SpecialHeader").isEmpty();
    }

    private static Mono<ServerResponse> headersHandler(ServerRequest req) {
        return ServerResponse.ok().body(Mono.just("Headers request"), String.class);
    }

    private static Mono<ServerResponse> htmlHandler(ServerRequest req) {
        return ServerResponse.ok().body(Mono.just("Here's your html file"), String.class);
    }

    private static Mono<ServerResponse> pdfHandler(ServerRequest req) {
        return ServerResponse.ok().body(Mono.just("<best><wurst>Best of the wurst</wurst><best>"), String.class);
    }

    private static Mono<ServerResponse> badStatusCode(ServerRequest req) {
        return ServerResponse.status(463).body(Mono.just("bad response code"), String.class);
    }

    private static HandlerFunction<ServerResponse> makeWebClientHandler(URI serverEndpoint) {
        return request -> {
            try {
                URI uri = new URI(serverEndpoint.getScheme(), serverEndpoint.getUserInfo(), serverEndpoint.getHost(), serverEndpoint.getPort(),
                        serverEndpoint.getPath(), String.format("%s=1", HttpTestServer.NO_TRANSACTION), null);

                final String response = makeGetRequest(uri).block().bodyToMono(String.class).block();
                return ServerResponse.ok().body(Mono.just("WebClient request complete! Request body size: " + response.length()), String.class);
            } catch (Throwable t) {
                return ServerResponse.ok().body(Mono.just("Failed to make WebClient request"), String.class);
            }
        };
    }

    private static Mono<ClientResponse> makeGetRequest(URI uri) {
        WebClient webClient = WebClient.builder().build();
        return webClient.get().uri(uri).exchange();
    }

}
