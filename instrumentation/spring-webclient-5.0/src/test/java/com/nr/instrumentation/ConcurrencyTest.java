/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.newrelic.agent.introspec.MetricsHelper.getUnscopedMetricCount;
import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.springframework"})
public class ConcurrencyTest {
    private ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testParallelWebClientCalls() throws Exception {
        int nIterations = 1000;
        waitAll(executor.submit(new WebClientCaller(nIterations, "http://host0.com")),
                executor.submit(new WebClientCaller(nIterations, "http://host1.com")));
        assertEquals(nIterations, getUnscopedMetricCount("External/host0.com/Spring-WebClient/exchange"));
        assertEquals(nIterations, getUnscopedMetricCount("External/host1.com/Spring-WebClient/exchange"));
    }

    public void waitAll(Future<?>... futures) throws Exception {
        for (Future<?> future : futures)
            future.get();
    }

    public static class WebClientCaller implements Runnable {
        final int count;
        final String url;

        public WebClientCaller(int count, String url) {
            this.count = count;
            this.url = url;
        }

        @Override
        public void run() {
            int remainingCalls = count;
            while (remainingCalls-- > 0) {
                makeGetRequest(URI.create(url)).block();
            }
        }

        @Trace(dispatcher = true)
        public Mono<ClientResponse> makeGetRequest(URI uri) {
            WebClient webClient = WebClient.builder()
                    .exchangeFunction(new ExchangeFunctionStub())
                    .build();
            return webClient.get().uri(uri).exchange();
        }
    }

    public static class ExchangeFunctionStub implements ExchangeFunction {
        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            return Mono.just(new ClientResponseStub());
        }
    }

    public static class ClientResponseStub implements ClientResponse {

        @Override
        public HttpStatus statusCode() {
            return HttpStatus.OK;
        }

        @Override
        public Headers headers() {
            return new Headers() {
                @Override
                public OptionalLong contentLength() {
                    return OptionalLong.of(0L);
                }

                @Override
                public Optional<MediaType> contentType() {
                    return Optional.empty();
                }

                @Override
                public List<String> header(String headerName) {
                    return Collections.emptyList();
                }

                @Override
                public HttpHeaders asHttpHeaders() {
                    return new HttpHeaders();
                }
            };
        }

        @Override
        public MultiValueMap<String, ResponseCookie> cookies() {
            return null;
        }

        @Override
        public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
            return null;
        }

        @Override
        public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
            return null;
        }

        @Override
        public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
            return null;
        }

        @Override
        public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
            return null;
        }

        @Override
        public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
            return null;
        }

        @Override
        public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
            return null;
        }

        @Override
        public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> typeReference) {
            return null;
        }

        @Override
        public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementType) {
            return null;
        }

        @Override
        public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> typeReference) {
            return null;
        }
    }
}
