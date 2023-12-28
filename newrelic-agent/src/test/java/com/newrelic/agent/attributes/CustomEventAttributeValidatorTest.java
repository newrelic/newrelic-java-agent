/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomEventAttributeValidatorTest {

    final String methodCalled = "method";
    final static String ATTRIBUTE_TYPE = "custom";
    final static int ATTR_SIZE = 1024;

    @BeforeClass
    public static void beforeClass() {
        MockServiceManager sm = new MockServiceManager();
        ServiceFactory.setServiceManager(sm);
        AgentConfig agentConfig = mock(AgentConfig.class, RETURNS_DEEP_STUBS);
        MockConfigService configService = new MockConfigService(agentConfig);
        sm.setConfigService(configService);

        when(agentConfig.getInsightsConfig().getMaxAttributeValue())
                .thenReturn(ATTR_SIZE);
    }

    @Test
    public void testVerifyTruncatedValue() {
        Map<String, Object> input = new HashMap<>();
        String longValue = Strings.repeat("e", ATTR_SIZE + 42);
        String longExpectedValue = Strings.repeat("e", ATTR_SIZE);
        input.put("key", longValue);
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.of("apple", "pie", "sugar", "cream", "key", longExpectedValue);

        AttributeValidator attributeValidator = new CustomEventAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }
}
