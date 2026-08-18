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
import io.opentelemetry.exporter.internal.http.HttpSender;
import junit.framework.TestCase;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
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

        FakeHttpSenderResponse response = new FakeHttpSenderResponse(200, new byte[] { 1, 2, 3 });

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            OtlpAuditLogger.logAuditResponse(response);
        }

        // should log the "Received OTLP/Metrics response: status={0}, bytes={1}, payload: {2}" message format
        verify(logger).log(eq(Level.INFO), anyString(), eq(200), eq(3), anyString());
    }

    public void testLogAuditResponseHandlesNullBody() {
        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);

        FakeHttpSenderResponse response = new FakeHttpSenderResponse(500, null);

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            OtlpAuditLogger.logAuditResponse(response);
        }

        // should log the "Received OTLP/Metrics response: status={0}, bytes={1}, payload: {2}" message format
        verify(logger).log(eq(Level.INFO), anyString(), eq(500), eq(0), anyString());
    }

    public void testLogAuditResponseSilentlyIgnoresInvalidResponse() {
        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);

        BadFakeHttpSenderResponse response = new BadFakeHttpSenderResponse(400, null);

        try (MockedStatic<NewRelic> mocked = Mockito.mockStatic(NewRelic.class)) {
            mocked.when(NewRelic::getAgent).thenReturn(agent);
            OtlpAuditLogger.logAuditResponse(response);
        }

        // should log the "Error logging OTLP/Metrics response" message format
        verify(logger).log(eq(Level.INFO), anyString());
    }

    static class FakeHttpSenderResponse implements HttpSender.Response {
        private final int code;
        private final byte[] body;

        FakeHttpSenderResponse(int code, byte[] body) {
            this.code = code;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return code;
        }

        @Override
        public String statusMessage() {
            return "Not used";
        }

        @Override
        public byte[] responseBody() throws IOException {
            return body;
        }
    }

    static class BadFakeHttpSenderResponse implements HttpSender.Response {
        private final int code;
        private final byte[] body;

        BadFakeHttpSenderResponse(int code, byte[] body) {
            this.code = code;
            this.body = body;
        }

        @Override
        public int statusCode() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String statusMessage() {
            return "Not used";
        }

        @Override
        public byte[] responseBody() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
