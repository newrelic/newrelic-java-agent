package com.newrelic.agent.aimonitoring;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.api.agent.LlmFeedbackEventAttributes;
import com.newrelic.api.agent.LlmTokenCountCallback;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AiMonitoringImplTest {

    @Mock
    private AiMonitoringImpl aiMonitoringImpl;

    private LlmTokenCountCallback callback;
    Map<String, Object> llmFeedbackEventAttributes;

    @Before
    public void setup() {
        String traceId = "123456";
        Integer rating = 5;
        LlmFeedbackEventAttributes.Builder llmFeedbackEventBuilder = new LlmFeedbackEventAttributes.Builder(traceId, rating);
        llmFeedbackEventAttributes = llmFeedbackEventBuilder
                .category("General")
                .message("Great experience")
                .build();
        callback = getCallback();
        aiMonitoringImpl = new AiMonitoringImpl();
    }

    @Test
    public void testRecordLlmFeedbackEventSent() {
        try {
            aiMonitoringImpl.recordLlmFeedbackEvent(llmFeedbackEventAttributes);
        } catch (IllegalArgumentException e) {
            // test should not catch an exception
        }

    }

    @Test
    public void testRecordLlmFeedbackEventWithNullAttributes() {

        try {
            aiMonitoringImpl.recordLlmFeedbackEvent(null);
            Assert.fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception thrown, test passed
            System.out.println("IllegalArgumentException successfully thrown!");
        }
    }

    @Test
    public void testCallbackSetSuccessfully() {
        aiMonitoringImpl.setLlmTokenCountCallback(callback);
        assertEquals(callback, LlmTokenCountCallbackHolder.getLlmTokenCountCallback());
    }

    @Test
    public void testSetLlmTokenCountCallbackWithNull() {

        try {
            aiMonitoringImpl.setLlmTokenCountCallback(null);
            Assert.fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception thrown, test passes
            System.out.println("IllegalArgumentException successfully thrown!");
        }

    }

    public LlmTokenCountCallback getCallback() {
        class TestCallback implements LlmTokenCountCallback {

            @Override
            public int calculateLlmTokenCount(String model, String content) {
                return 13;
            }
        }
        return new TestCallback();
    }

}
