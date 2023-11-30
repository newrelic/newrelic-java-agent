/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.opentelemetry;

import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;

class OpenTelemetryTracedMethodTest {

    @RegisterExtension
    static OpenTelemetryExtension openTelemetry = OpenTelemetryExtension.create();

    @BeforeEach
    void setup() {
        OpenTelemetryNewRelic.resetForTest();
        OpenTelemetryNewRelic.install(openTelemetry.getOpenTelemetry());
    }

    @AfterEach
    void cleanup() {
        OpenTelemetryNewRelic.resetForTest();
    }

    @ParameterizedTest
    @MethodSource("operationArgs")
    void operations(Runnable runnable, Consumer<SpanData> spanConsumer) {
        Span span = openTelemetry.getOpenTelemetry().getTracer("scopeName").spanBuilder("spanName").startSpan();
        try (Scope unused = span.makeCurrent()) {
            runnable.run();
        } finally {
            span.end();
        }

        assertThat(openTelemetry.getSpans())
                .satisfiesExactly(spanConsumer);
    }

    private static Stream<Arguments> operationArgs() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("double_key2", 2.2f);
        attributes.put("long_key2", 2);
        attributes.put("bool_key2", false);
        attributes.put("string_key2", "value2");
        return Stream.of(
                // OpenTelemetryTracedMethod API
                Arguments.of(
                        (Runnable) () -> {
                            OpenTelemetryNewRelic.getAgent().getTracedMethod().addCustomAttribute("double_key1", 1.1);
                            OpenTelemetryNewRelic.getAgent().getTracedMethod().addCustomAttribute("long_key1", 1L);
                            OpenTelemetryNewRelic.getAgent().getTracedMethod().addCustomAttribute("string_key1", "value1");
                            OpenTelemetryNewRelic.getAgent().getTracedMethod().addCustomAttribute("bool_key1", true);
                            OpenTelemetryNewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
                        },
                        spanAssert(span -> assertThat(span)
                                .hasAttributesSatisfying(
                                        equalTo(AttributeKey.doubleKey("double_key1"), 1.1),
                                        equalTo(AttributeKey.longKey("long_key1"), 1),
                                        equalTo(AttributeKey.stringKey("string_key1"), "value1"),
                                        equalTo(AttributeKey.booleanKey("bool_key1"), true),
                                        satisfies(AttributeKey.doubleKey("double_key2"), value -> value.isCloseTo(2.2, Offset.offset(0.01))),
                                        equalTo(AttributeKey.longKey("long_key2"), 2),
                                        equalTo(AttributeKey.stringKey("string_key2"), "value2"),
                                        equalTo(AttributeKey.booleanKey("bool_key2"), false)
                                )))
        );
    }

    private static Consumer<SpanData> spanAssert(Consumer<SpanData> spanConsumer) {
        return spanConsumer;
    }

}
