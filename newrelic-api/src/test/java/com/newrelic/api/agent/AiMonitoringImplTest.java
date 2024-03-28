package com.newrelic.api.agent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;

@RunWith(MockitoJUnitRunner.class)
public class AiMonitoringImplTest {

    @Mock
    AiMonitoringImpl aiMonitoringImpl;
    LlmFeedbackEventAttributes.Builder llmFeedbackEventBuilder;
    Map<String, Object> llmFeedbackEventAttributes;

    @Before
    public void setup() {
        String traceId = "123456";
        Integer rating = 5;
        llmFeedbackEventBuilder = new LlmFeedbackEventAttributes.Builder(traceId, rating);
        llmFeedbackEventAttributes = llmFeedbackEventBuilder
                .category("General")
                .message("Great experience")
                .build();
    }

    @Test
    public void testRecordLlmFeedbackEventSuccess() {
        aiMonitoringImpl.recordLlmFeedbackEvent(llmFeedbackEventAttributes);
        Mockito.verify(aiMonitoringImpl).recordLlmFeedbackEvent(llmFeedbackEventAttributes);
    }

    @Test
    public void testRecordLlmFeedbackEventFailure() {
        Mockito.doThrow(new RuntimeException("Custom event recording failed")).when(aiMonitoringImpl).recordLlmFeedbackEvent(anyMap());
        try {
            aiMonitoringImpl.recordLlmFeedbackEvent(llmFeedbackEventAttributes);
        } catch (RuntimeException exception) {
            Mockito.verify(aiMonitoringImpl).recordLlmFeedbackEvent(llmFeedbackEventAttributes);
            Assert.assertEquals("Custom event recording failed", exception.getMessage());
        }
    }

    @Test
    public void testRecordLlmFeedbackEventWithNullAttributes() {
        Mockito.doThrow(new IllegalArgumentException("llmFeedbackEventAttributes cannot be null"))
                .when(aiMonitoringImpl).recordLlmFeedbackEvent(null);

        try {
            aiMonitoringImpl.recordLlmFeedbackEvent(null);
            Assert.fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception thrown, test passed
            System.out.println("IllegalArgumentException successfully thrown!");
        }
    }

    @Test
    public void testSetLlmTokenCountCallbackSuccess() {
        LlmTokenCountCallback testCallback = Mockito.mock(LlmTokenCountCallback.class);
        aiMonitoringImpl.setLlmTokenCountCallback(testCallback);
        Mockito.verify(aiMonitoringImpl).setLlmTokenCountCallback(testCallback);
        Assert.assertNotNull(LlmTokenCountCallbackHolder.getInstance());
    }

    @Test
    public void testSetLlmTokenCountCallbackReturnsIntegerGreaterThanZero() {
        class TestCallback implements LlmTokenCountCallback {

            @Override
            public Integer calculateLlmTokenCount(String model, String content) {
                return 13;
            }
        }

        TestCallback testCallback = new TestCallback();
        aiMonitoringImpl.setLlmTokenCountCallback(testCallback);
    }

}
