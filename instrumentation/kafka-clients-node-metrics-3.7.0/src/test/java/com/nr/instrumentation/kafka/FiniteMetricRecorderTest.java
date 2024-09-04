package com.nr.instrumentation.kafka;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import com.newrelic.api.agent.NewRelic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class FiniteMetricRecorderTest {

    private FiniteMetricRecorder recorder;
    private static final String METRIC = "metric";
    private static final float VALUE = 42.0f;
    private static final int COUNT = 11;


    @Before
    public void setUp() {
        recorder = new FiniteMetricRecorder();
    }

    @Test
    public void incrementCounter() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            recorder.incrementCounter(METRIC, COUNT);
            newRelic.verify(() -> NewRelic.incrementCounter(eq(METRIC), eq(COUNT)));
        }
    }

    @Test
    public void tryRecordMetric() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            boolean returnValue = recorder.tryRecordMetric(METRIC, VALUE);

            assertThat(returnValue, is(true));
            newRelic.verify(() -> NewRelic.recordMetric(eq(METRIC), eq(VALUE)));
        }
    }

    @Test
    public void tryRecordInfiniteMetric() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            boolean returnValue = recorder.tryRecordMetric(METRIC, Float.POSITIVE_INFINITY);

            assertThat(returnValue, is(false));
            newRelic.verifyNoInteractions();
        }
    }

    @Test
    public void recordMetric() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            recorder.recordMetric(METRIC, VALUE);
            newRelic.verify(() -> NewRelic.recordMetric(eq(METRIC), eq(VALUE)));
        }
    }

    @Test
    public void recordInfiniteMetric() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            recorder.tryRecordMetric(METRIC, Float.POSITIVE_INFINITY);
            newRelic.verifyNoInteractions();
        }
    }

}