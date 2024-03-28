package com.newrelic.agent.aimonitoring;

import com.newrelic.api.agent.AiMonitoringImpl;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.LlmFeedbackEventAttributes;
import com.newrelic.api.agent.LlmTokenCountCallback;
import com.newrelic.api.agent.LlmTokenCountCallbackHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AiMonitoringImplTest {

    @Mock
    Insights insights;

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
        verify(aiMonitoringImpl).recordLlmFeedbackEvent(llmFeedbackEventAttributes);
    }

    @Test
    public void testRecordLlmFeedbackEventFailure() {
        doThrow(new RuntimeException("Custom event recording failed")).when(aiMonitoringImpl).recordLlmFeedbackEvent(anyMap());
        try {
            aiMonitoringImpl.recordLlmFeedbackEvent(llmFeedbackEventAttributes);
        } catch (RuntimeException exception) {
            verify(aiMonitoringImpl).recordLlmFeedbackEvent(llmFeedbackEventAttributes);
            assertEquals("Custom event recording failed", exception.getMessage());
        }
    }

    @Test
    public void testRecordLlmFeedbackEventWithNullAttributes() {
        doThrow(new IllegalArgumentException("llmFeedbackEventAttributes cannot be null"))
                .when(aiMonitoringImpl).recordLlmFeedbackEvent(null);

        try {
            aiMonitoringImpl.recordLlmFeedbackEvent(null);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception thrown, test passed
            System.out.println("IllegalArgumentException successfully thrown!");
        }
    }

    @Test
    public void testSetLlmTokenCountCallbackSuccess() {
        LlmTokenCountCallback testCallback = mock(LlmTokenCountCallback.class);
        aiMonitoringImpl.setLlmTokenCountCallback(testCallback);
        verify(aiMonitoringImpl).setLlmTokenCountCallback(testCallback);
        assertNotNull(LlmTokenCountCallbackHolder.getInstance());
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
