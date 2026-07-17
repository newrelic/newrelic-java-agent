/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.base.Strings;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LlmEventAttributeValidatorTest {

    final String methodCalled = "method";
    final static String ATTRIBUTE_TYPE = "llm";
    final static int ATTR_SIZE = 255;

    @BeforeClass
    public static void beforeClass() {
        MockServiceManager sm = new MockServiceManager();
        ServiceFactory.setServiceManager(sm);
        AgentConfig agentConfig = mock(AgentConfig.class, RETURNS_DEEP_STUBS);
        MockConfigService configService = new MockConfigService(agentConfig);
        sm.setConfigService(configService);

        when(agentConfig.getInsightsConfig().getMaxAttributeValue()).thenReturn(ATTR_SIZE);
    }

    @Test
    public void testContentIsNotTruncated() {
        String longValue = Strings.repeat("e", ATTR_SIZE + 42);

        AttributeValidator attributeValidator = new LlmEventAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);
        String result = attributeValidator.verifyParameterAndReturnValue("content", longValue, methodCalled);

        assertEquals(longValue, result);
    }

    @Test
    public void testInputIsNotTruncated() {
        String longValue = Strings.repeat("e", ATTR_SIZE + 42);

        AttributeValidator attributeValidator = new LlmEventAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);
        String result = attributeValidator.verifyParameterAndReturnValue("input", longValue, methodCalled);

        assertEquals(longValue, result);
    }

    @Test
    public void testReasoningContentIsNotTruncated() {
        String longValue = Strings.repeat("e", ATTR_SIZE + 42);

        AttributeValidator attributeValidator = new LlmEventAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);
        String result = attributeValidator.verifyParameterAndReturnValue("reasoning_content", longValue, methodCalled);

        assertEquals(longValue, result);
    }

    @Test
    public void testReasoningContentSignatureIsNotTruncated() {
        String longValue = Strings.repeat("e", ATTR_SIZE + 42);

        AttributeValidator attributeValidator = new LlmEventAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);
        String result = attributeValidator.verifyParameterAndReturnValue("reasoning_content_signature", longValue, methodCalled);

        assertEquals(longValue, result);
    }

    @Test
    public void testOtherAttributesAreStillTruncated() {
        String longValue = Strings.repeat("e", ATTR_SIZE + 42);
        String expectedTruncatedValue = Strings.repeat("e", ATTR_SIZE);

        AttributeValidator attributeValidator = new LlmEventAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);
        String result = attributeValidator.verifyParameterAndReturnValue("role", longValue, methodCalled);

        assertEquals(expectedTruncatedValue, result);
    }
}
