package com.newrelic.api.agent.opentelemetry;

import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenTelemetryMeterTest {
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

    @Test
    public void newCounter() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("region", "EU");
        OpenTelemetryNewRelic.getAgent().getMeter().newCounter("custom.test.counter").add(50, attributes);
        OpenTelemetryNewRelic.getAgent().getMeter().newCounter("custom.test.counter").add(35, attributes);

        assertThat(openTelemetry.getMetrics())
                .satisfiesExactlyInAnyOrder(
                        metricData -> OpenTelemetryAssertions.assertThat(metricData).hasName("custom.test.counter")
                                .hasLongSumSatisfying(sum -> sum.hasPointsSatisfying(
                                        point -> point.hasAttributes(Attributes.builder().put("region", "EU").build())
                                                .hasValue(85)
                                ))
                );
    }

    @Test
    public void newSummary() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("region", "EU");
        OpenTelemetryNewRelic.getAgent().getMeter().newSummary("custom.test.sum").add(5.0, attributes);
        OpenTelemetryNewRelic.getAgent().getMeter().newSummary("custom.test.sum").add(8.0, attributes);

        assertThat(openTelemetry.getMetrics())
                .satisfiesExactlyInAnyOrder(
                        metricData -> OpenTelemetryAssertions.assertThat(metricData).hasName("custom.test.sum")
                                .hasHistogramSatisfying(sum -> sum.hasPointsSatisfying(
                                        point -> point.hasAttributes(Attributes.builder().put("region", "EU").build())
                                                .hasSum(13.0d).hasCount(2)
                                ))
                );
    }
}