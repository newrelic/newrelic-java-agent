package com.newrelic.api.agent;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class AiMonitoringImplTest {

    LlmFeedbackEventAttributes.Builder llmFeedbackEventBuilder;
    AiMonitoringImpl aiMonitoringImpl;
    Map<String, Object> llmFeedbackEventParameters;

    @Before
    public void setup() {
        String traceId = "123456";
        Integer rating = 5;
        llmFeedbackEventBuilder = new LlmFeedbackEventAttributes.Builder(traceId, rating);
        aiMonitoringImpl = new AiMonitoringImpl();
    }

    @Test
    public void testRecordLlmFeedbackEvent() {
        llmFeedbackEventParameters = llmFeedbackEventBuilder
                .category("General")
                .message("Great experience")
                .build();

        aiMonitoringImpl.recordLlmFeedbackEvent(llmFeedbackEventParameters);

        // TODO: verify recordCustomEvent was called with the correct parameters
    }

    @Test
    public void testRecordLlmFeedbackEvent_NullMap() {
        // TODO: invoke the method with a null map
        // TODO: verify recordCustomEvent was not called
    }

    @Test
    public void testRecordLlmFeedbackEvent_EmptyMap() {
        // TODO: invoke the method with an empty map
        // TODO: verify recordCustomEvent was not called
    }

    @Test
    public void testSetLlmTokenCountCallbackReturnsIntegerGreaterThanZero() {
        class TestCallback implements LlmTokenCountCallback {

            @Override
            public Integer calculateLlmTokenCount(String model, String content) {
                return -5;
            }
        }

        TestCallback testCallback = new TestCallback();
        aiMonitoringImpl.setLlmTokenCountCallback(testCallback);
    }

}
