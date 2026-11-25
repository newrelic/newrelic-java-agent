/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.MaxPayloadException;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.transport.HttpClientWrapper;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.transport.ReadResult;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OtlpDataSenderTest {

    private static final String LICENSE_KEY = "eu01xxlicensekey";

    private StatsService statsService;
    private HttpClientWrapper httpClientWrapper;

    @Before
    public void before() throws Exception {
        statsService = mock(StatsService.class);
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setStatsService(statsService);
        httpClientWrapper = mock(HttpClientWrapper.class);
        respondWith(200);
    }

    @After
    public void after() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void sendsLogsAsGzippedProtobufToLogsPath() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("otlp.eu01.nr-data.net",
                LICENSE_KEY, false, httpClientWrapper);

        sender.sendLogEvents(Collections.singletonMap("service.name", "My App"), Collections.singletonList(logEvent("hello")));

        HttpClientWrapper.Request request = captureRequest();
        assertEquals("https://otlp.eu01.nr-data.net/v1/logs", request.getURL().toString());
        assertEquals(HttpClientWrapper.Verb.POST, request.getVerb());
        assertEquals("gzip", request.getEncoding());
        assertEquals("application/x-protobuf", request.getRequestMetadata().get("Content-Type"));
        assertEquals(LICENSE_KEY, request.getRequestMetadata().get("api-key"));

        ExportLogsServiceRequest decoded = ExportLogsServiceRequest.parseFrom(gunzip(request.getData()));
        assertEquals("hello", decoded.getResourceLogs(0).getScopeLogs(0).getLogRecords(0).getBody().getStringValue());
        verifyMetricRecorded("Supportability/Java/OTLP/HttpCode/200");
        verifyMetricRecorded("Supportability/Java/OTLP/Output/Bytes");
        verifyMetricRecorded("Supportability/Java/OTLP/Logs/Output/Bytes");
    }

    @Test
    public void auditRequestMessageHasDestinationByteCountAndBase64Payload() throws Exception {
        byte[] payload = new byte[2048];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) i;
        }

        String message = OtlpDataSender.auditRequestMessage("Logs", new URL("https://otlp.nr-data.net/v1/logs"), payload);

        // The byte count is the decoded size, written without thousands separators, and the payload decodes back to the original bytes
        assertEquals("Sent OTLP/Logs to: https://otlp.nr-data.net/v1/logs, bytes: 2048, payload: "
                + Base64.getEncoder().encodeToString(payload), message);
    }

    @Test
    public void auditResponseMessageHasStatusByteCountAndBase64Body() {
        String message = OtlpDataSender.auditResponseMessage("Logs", 429, "slow down");

        assertEquals("Received OTLP/Logs response: status=429, bytes=9, payload: "
                + Base64.getEncoder().encodeToString("slow down".getBytes(StandardCharsets.UTF_8)), message);
    }

    @Test
    public void auditResponseMessageHandlesEmptyBody() {
        assertEquals("Received OTLP/Logs response: status=200, bytes=0, payload: ", OtlpDataSender.auditResponseMessage("Logs", 200, null));
        assertEquals("Received OTLP/Logs response: status=200, bytes=0, payload: ", OtlpDataSender.auditResponseMessage("Logs", 200, ""));
    }

    @Test
    public void auditModeSendsTheRequestAndDoesNotChangeTheResult() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net", LICENSE_KEY, true, httpClientWrapper);

        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("hello")));

        assertEquals("https://otlp.nr-data.net/v1/logs", captureRequest().getURL().toString());
    }

    @Test
    public void auditModeIsCheckedOnEverySend() throws Exception {
        final AtomicBoolean auditMode = new AtomicBoolean(false);
        final int[] checks = { 0 };
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net", LICENSE_KEY, () -> {
            checks[0]++;
            return auditMode.get();
        }, httpClientWrapper);

        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("first")));
        auditMode.set(true);
        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("second")));

        assertEquals(2, checks[0]);
    }

    @Test
    public void failureCheckingAuditModeDoesNotStopTheSend() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net", LICENSE_KEY, () -> {
            throw new IllegalStateException("config unavailable");
        }, httpClientWrapper);

        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("hello")));

        assertEquals("https://otlp.nr-data.net/v1/logs", captureRequest().getURL().toString());
    }

    @Test
    public void auditModeIsLoggedOnFirstSendAndWhenItChanges() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net", LICENSE_KEY, false, httpClientWrapper);

        assertTrue(sender.logAuditModeChange(false));
        assertFalse(sender.logAuditModeChange(false));
        assertTrue(sender.logAuditModeChange(true));
        assertFalse(sender.logAuditModeChange(true));
        assertTrue(sender.logAuditModeChange(false));
    }

    @Test
    public void auditEndpointsFilterWhichEndpointsAreAudited() {
        String logs = OtlpDataSender.AUDIT_ENDPOINT_LOGS;

        // audit_mode off: nothing is audited
        assertFalse(OtlpDataSender.shouldAudit(false, Collections.<String>emptySet(), logs));
        assertFalse(OtlpDataSender.shouldAudit(false, new HashSet<>(Arrays.asList(logs)), logs));
        // No endpoints configured (including the legacy "audit_mode: true", where there is no endpoint set): everything is audited
        assertTrue(OtlpDataSender.shouldAudit(true, null, logs));
        assertTrue(OtlpDataSender.shouldAudit(true, Collections.<String>emptySet(), logs));
        // Endpoints configured: only the listed ones are audited
        assertTrue(OtlpDataSender.shouldAudit(true, new HashSet<>(Arrays.asList("connect", logs)), logs));
        assertFalse(OtlpDataSender.shouldAudit(true, new HashSet<>(Arrays.asList("connect", "log_event_data")), logs));
    }

    @Test
    public void licenseKeyIsOnlySentToNewRelicEndpoints() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("unused.example.com", LICENSE_KEY, false, httpClientWrapper);

        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("hello")));

        List<HttpClientWrapper.Request> requests = captureRequests(1);
        assertFalse(requests.get(0).getRequestMetadata().containsKey("api-key"));
        assertEquals("application/x-protobuf", requests.get(0).getRequestMetadata().get("Content-Type"));
    }

    @Test
    public void newRelicHostDetection() {
        assertTrue(OtlpDataSender.isNewRelicHost("otlp.nr-data.net"));
        assertTrue(OtlpDataSender.isNewRelicHost("OTLP.EU01.NR-DATA.NET"));
        assertTrue(OtlpDataSender.isNewRelicHost("gov-otlp.nr-data.net"));
        assertFalse(OtlpDataSender.isNewRelicHost("nr-data.net.example.com"));
        assertFalse(OtlpDataSender.isNewRelicHost("localhost"));
        assertFalse(OtlpDataSender.isNewRelicHost(null));
    }

    @Test
    public void emptyBatchesAreNotSent() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net",
                LICENSE_KEY, false, httpClientWrapper);

        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.<LogEvent>emptyList());

        verify(httpClientWrapper, never()).execute(any(HttpClientWrapper.Request.class), any(HttpClientWrapper.ExecuteEventHandler.class));
    }

    @Test
    public void acceptedResponseIsSuccess() throws Exception {
        respondWith(202);
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net",
                LICENSE_KEY, false, httpClientWrapper);

        sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("hello")));

        verifyMetricRecorded("Supportability/Java/OTLP/HttpCode/202");
    }

    @Test
    public void errorResponsesThrowHttpErrorWithHarvestRetrySemantics() throws Exception {
        assertHttpError(429, false);
        assertHttpError(503, false);
        assertHttpError(400, true);
        assertHttpError(413, true);
    }

    @Test
    public void payloadAtMaxSizeIsDropped() throws Exception {
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net",
                LICENSE_KEY, false, httpClientWrapper);

        // Random messages don't compress, so this batch is larger than the maximum payload size after gzip
        Random random = new Random(42);
        List<LogEvent> events = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            byte[] message = new byte[4096];
            random.nextBytes(message);
            events.add(logEvent(new String(message, "ISO-8859-1")));
        }

        try {
            sender.sendLogEvents(Collections.<String, Object>emptyMap(), events);
            fail("Expected MaxPayloadException");
        } catch (MaxPayloadException expected) {
            assertTrue(expected.getMessage().contains("Logs"));
        }
        verify(httpClientWrapper, never()).execute(any(HttpClientWrapper.Request.class), any(HttpClientWrapper.ExecuteEventHandler.class));
        verifyMetricRecorded("Supportability/Java/OTLP/MaxPayloadSizeLimit/Logs");
    }

    private void assertHttpError(int statusCode, boolean discardHarvestData) throws Exception {
        respondWith(statusCode);
        OtlpDataSender sender = new OtlpDataSender("otlp.nr-data.net",
                LICENSE_KEY, false, httpClientWrapper);
        try {
            sender.sendLogEvents(Collections.<String, Object>emptyMap(), Collections.singletonList(logEvent("hello")));
            fail("Expected HttpError for status " + statusCode);
        } catch (HttpError expected) {
            assertEquals(statusCode, expected.getStatusCode());
            assertEquals(discardHarvestData, expected.discardHarvestData());
        }
    }

    private void respondWith(int statusCode) throws Exception {
        when(httpClientWrapper.execute(any(HttpClientWrapper.Request.class), any(HttpClientWrapper.ExecuteEventHandler.class)))
                .thenReturn(ReadResult.create(statusCode, "", null));
    }

    private HttpClientWrapper.Request captureRequest() throws Exception {
        ArgumentCaptor<HttpClientWrapper.Request> captor = ArgumentCaptor.forClass(HttpClientWrapper.Request.class);
        verify(httpClientWrapper).execute(captor.capture(), any(HttpClientWrapper.ExecuteEventHandler.class));
        return captor.getValue();
    }

    private List<HttpClientWrapper.Request> captureRequests(int expectedCount) throws Exception {
        ArgumentCaptor<HttpClientWrapper.Request> captor = ArgumentCaptor.forClass(HttpClientWrapper.Request.class);
        verify(httpClientWrapper, times(expectedCount)).execute(captor.capture(), any(HttpClientWrapper.ExecuteEventHandler.class));
        return captor.getAllValues();
    }

    private void verifyMetricRecorded(String metricName) {
        verify(statsService).doStatsWork(any(StatsWork.class), eq(metricName));
    }

    private static LogEvent logEvent(String message) {
        return new LogEvent(Collections.<String, Object>singletonMap("message", message), 1.0f);
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(data))) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
