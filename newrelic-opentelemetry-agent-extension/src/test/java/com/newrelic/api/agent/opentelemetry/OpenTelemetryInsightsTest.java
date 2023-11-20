/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.opentelemetry;

import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;

class OpenTelemetryInsightsTest {

    private InMemoryLogRecordExporter exporter;
    private SdkLoggerProvider loggerProvider;

    @BeforeEach
    void setup() {
        exporter = InMemoryLogRecordExporter.create();
        loggerProvider = SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build();
        OpenTelemetryNewRelic.resetForTest();
        OpenTelemetryNewRelic.install(OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build());
    }

    @AfterEach
    void cleanup() {
        OpenTelemetryNewRelic.resetForTest();
    }

    @Test
    void recordCustomEvent() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("double_key", 1.1f);
        attributes.put("long_key", 1);
        attributes.put("bool_key", true);
        attributes.put("string_key", "value");

        OpenTelemetryNewRelic.getAgent().getInsights().recordCustomEvent("eventType", attributes);

        loggerProvider.forceFlush().join(10, TimeUnit.SECONDS);

        assertThat(exporter.getFinishedLogRecordItems())
                .satisfiesExactly(logRecordData -> assertThat(logRecordData)
                        .hasAttributesSatisfying(
                                equalTo(AttributeKey.stringKey("event.domain"), "newrelic.agent_api"),
                                equalTo(AttributeKey.stringKey("event.name"), "eventType"),
                                satisfies(AttributeKey.doubleKey("double_key"), value -> value.isCloseTo(1.1, Offset.offset(0.01))),
                                equalTo(AttributeKey.longKey("long_key"), 1),
                                equalTo(AttributeKey.stringKey("string_key"), "value"),
                                equalTo(AttributeKey.booleanKey("bool_key"), true)
                        ));
    }

}
