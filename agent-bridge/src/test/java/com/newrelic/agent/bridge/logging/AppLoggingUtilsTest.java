/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.logging;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AppLoggingUtilsTest {

    @Test
    public void testUrlEncoding() {
        final String ENCODED_PIPE = "%7C";
        final String ENCODED_SPACE = "+";
        // The main goal of the encoding is to eliminate | characters from the entity.name as | is used as
        // the BLOB_DELIMITER for separating the agent metadata attributes that are appended to log files
        final String valueToEncode = "|My Application|";
        final String expectedEncodedValue = ENCODED_PIPE + "My" + ENCODED_SPACE + "Application" + ENCODED_PIPE;

        String encodedValue = AppLoggingUtils.urlEncode(valueToEncode);

        Assert.assertEquals(expectedEncodedValue, encodedValue);
    }

    @Test
    public void urlEncode_withEncodingError_returnsOriginalString() {
        final String valueToEncode = "|My Application|";

        try (MockedStatic<URLEncoder> mockUrlEncoder = Mockito.mockStatic(URLEncoder.class)) {
            mockUrlEncoder.when(() -> URLEncoder.encode(valueToEncode, StandardCharsets.UTF_8.toString())).thenThrow(UnsupportedEncodingException.class);

            Assert.assertEquals(valueToEncode, AppLoggingUtils.urlEncode(valueToEncode));
        }
    }

    @Test
    public void getLinkingMetadataBlob_withNonNullMetadata_createsProperlyFormattedBlob() {
        Agent mockAgent = Mockito.mock(Agent.class);

        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(AppLoggingUtils.ENTITY_GUID, "1234");
        metadataMap.put(AppLoggingUtils.HOSTNAME, "host");
        metadataMap.put(AppLoggingUtils.TRACE_ID, "9876");
        metadataMap.put(AppLoggingUtils.SPAN_ID, "567");
        metadataMap.put(AppLoggingUtils.ENTITY_NAME, "name");

        Mockito.when(mockAgent.getLinkingMetadata()).thenReturn(metadataMap);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertEquals(" NR-LINKING|1234|host|9876|567|name|", AppLoggingUtils.getLinkingMetadataBlob());
        }
    }

    @Test
    public void getLinkingMetadataBlob_withNullMetadata_createsSparseBlob() {
        Agent mockAgent = Mockito.mock(Agent.class);

        Mockito.when(mockAgent.getLinkingMetadata()).thenReturn(null);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertEquals(" NR-LINKING|", AppLoggingUtils.getLinkingMetadataBlob());
        }
    }

    @Test
    public void isApplicationLoggingEnabled_returnsTrueIfEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.enabled", true)).thenReturn(true);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertTrue(AppLoggingUtils.isApplicationLoggingEnabled());
        }
    }

    @Test
    public void isApplicationLoggingEnabled_returnsFalseIfDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.enabled", true)).thenReturn(false);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertFalse(AppLoggingUtils.isApplicationLoggingEnabled());
        }
    }
    @Test
    public void isApplicationLoggingMetricsEnabled_returnsTrueIfEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.metrics.enabled", true)).thenReturn(true);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertTrue(AppLoggingUtils.isApplicationLoggingMetricsEnabled());
        }
    }

    @Test
    public void isApplicationLoggingMetricsEnabled_returnsFalseIfDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.metrics.enabled", true)).thenReturn(false);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertFalse(AppLoggingUtils.isApplicationLoggingMetricsEnabled());
        }
    }
    @Test
    public void isApplicationLoggingForwardingEnabled_returnsTrueIfEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.forwarding.enabled", true)).thenReturn(true);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertTrue(AppLoggingUtils.isApplicationLoggingForwardingEnabled());
        }
    }

    @Test
    public void isApplicationLoggingForwardingEnabled_returnsFalseIfDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.forwarding.enabled", true)).thenReturn(false);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertFalse(AppLoggingUtils.isApplicationLoggingForwardingEnabled());
        }
    }
    @Test
    public void isApplicationLoggingLocalDecoratingEnabled_returnsTrueIfEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.local_decorating.enabled", false)).thenReturn(true);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertTrue(AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled());
        }
    }

    @Test
    public void isApplicationLoggingLocalDecoratingEnabled_returnsFalseIfDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.local_decorating.enabled", false)).thenReturn(false);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertFalse(AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled());
        }
    }
    @Test
    public void isAppLoggingContextDataEnabled_returnsTrueIfEnabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.forwarding.context_data.enabled", false)).thenReturn(true);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertTrue(AppLoggingUtils.isAppLoggingContextDataEnabled());
        }
    }

    @Test
    public void isAppLoggingContextDataEnabled_returnsFalseIfDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(mockAgent.getConfig().getValue("application_logging.forwarding.context_data.enabled", false)).thenReturn(false);
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);

            Assert.assertFalse(AppLoggingUtils.isAppLoggingContextDataEnabled());
        }
    }
}