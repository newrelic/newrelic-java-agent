/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.aimonitoring;

import com.newrelic.api.agent.LlmTokenCountCallback;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LlmTokenCountResolverTest {

    @After
    public void tearDown() {
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(null);
    }

    @Test
    public void hasCompleteUsageData_allFieldsValid_returnsTrue() {
        assertCompleteUsageReturnsTrue(10, 20, 30);
        assertCompleteUsageReturnsTrue(0, 0, 0);
    }

    @Test
    public void hasCompleteUsageData_anyFieldNull_returnsFalse() {
        assertCompleteUsageReturnsFalse(null, 20, 30);
        assertCompleteUsageReturnsFalse(10, null, 30);
        assertCompleteUsageReturnsFalse(10, 20, null);
    }

    @Test
    public void hasCompleteUsageData_anyFieldNegative_returnsFalse() {
        assertCompleteUsageReturnsFalse(-1, 20, 30);
        assertCompleteUsageReturnsFalse(10, -1, 30);
        assertCompleteUsageReturnsFalse(10, 20, -1);
    }

    @Test
    public void hasCompleteUsageData_allFieldsNull_returnsFalse() {
        boolean result = LlmTokenCountResolver.hasCompleteUsageData(null, null, null);
        assertFalse(result);
    }

    @Test
    public void getMessageTokenCount_completeSummaryUsage_returnsZero() {
        assertMessageTokenCountEquals(0, true, "gpt-4", "content", null);
        assertMessageTokenCountEquals(0, true, "gpt-4", "content", new MockCallback(100));
    }

    @Test
    public void getMessageTokenCount_incompleteSummaryUsageWithCallback_returnsCallbackResult() {
        assertMessageTokenCountEquals(42, false, "gpt-4", "content", new MockCallback(42));
    }

    @Test
    public void getMessageTokenCount_incompleteSummaryUsageNoCallback_returnsNull() {
        Integer result = LlmTokenCountResolver.getMessageTokenCount(false, "gpt-4", "content");
        assertNull(result);
    }

    @Test
    public void getMessageTokenCount_invalidContent_returnsNull() {
        assertMessageTokenCountReturnsNull(false, "gpt-4", null, new MockCallback(100));
        assertMessageTokenCountReturnsNull(false, "gpt-4", "", new MockCallback(100));
    }

    @Test
    public void getMessageTokenCount_whitespaceContent_returnsCallbackResult() {
        assertMessageTokenCountEquals(5, false, "gpt-4", "   ", new MockCallback(5));
    }

    private void assertCompleteUsageReturnsTrue(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        boolean result = LlmTokenCountResolver.hasCompleteUsageData(promptTokens, completionTokens, totalTokens);
        assertTrue(result);
    }

    private void assertCompleteUsageReturnsFalse(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        boolean result = LlmTokenCountResolver.hasCompleteUsageData(promptTokens, completionTokens, totalTokens);
        assertFalse(result);
    }

    private void assertMessageTokenCountEquals(Integer expected, boolean completeSummaryUsage, String model, String content, LlmTokenCountCallback callback) {
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(callback);
        Integer result = LlmTokenCountResolver.getMessageTokenCount(completeSummaryUsage, model, content);
        assertEquals(expected, result);
    }

    private void assertMessageTokenCountReturnsNull(boolean completeSummaryUsage, String model, String content, LlmTokenCountCallback callback) {
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(callback);
        Integer result = LlmTokenCountResolver.getMessageTokenCount(completeSummaryUsage, model, content);
        assertNull(result);
    }

    /**
     * Mock callback that returns a fixed token count
     */
    private static class MockCallback implements LlmTokenCountCallback {
        private final int tokenCount;

        public MockCallback(int tokenCount) {
            this.tokenCount = tokenCount;
        }

        @Override
        public int calculateLlmTokenCount(String model, String content) {
            return tokenCount;
        }
    }
}