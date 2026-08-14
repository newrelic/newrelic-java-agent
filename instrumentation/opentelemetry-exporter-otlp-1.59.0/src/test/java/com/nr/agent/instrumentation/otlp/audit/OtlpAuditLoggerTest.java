/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.otlp.audit;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.sdk.common.export.HttpResponse;
import junit.framework.TestCase;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.logging.Level;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OtlpAuditLoggerTest extends TestCase {

    public void testIsAuditModeEnabledDefaultsFalse() {
        Agent agent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(agent.getConfig().getValue("audit_mode", false)).thenReturn(false);

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            assertFalse(OtlpAuditLogger.isAuditModeEnabled());
        }
    }

    public void testIsAuditModeEnabledReturnsTrue() {
        Agent agent = mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        when(agent.getConfig().getValue("audit_mode", false)).thenReturn(true);

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            assertTrue(OtlpAuditLogger.isAuditModeEnabled());
        }
    }

    public void testLogAuditResponseLogsStatusAndBody() {
        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);

        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn(new byte[] { 1, 2, 3 });

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            OtlpAuditLogger.logAuditResponse(response);
        }

        verify(logger).log(eq(Level.INFO), anyString(), eq(200), eq(3), anyString());
    }

    public void testLogAuditResponseHandlesNullBody() {
        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);

        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatusCode()).thenReturn(500);
        when(response.getResponseBody()).thenReturn(null);

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            OtlpAuditLogger.logAuditResponse(response);
        }

        verify(logger).log(eq(Level.INFO), anyString(), eq(500), eq(0), anyString());
    }
}
