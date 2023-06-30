package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.JarData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IntrospectorRPMServiceTest {

    @Mock
    private ErrorService errorService;

    @InjectMocks
    private IntrospectorRPMService introspectorRPMService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsEnabled() {
        assert(introspectorRPMService.isEnabled());
    }

    @Test
    public void testIsStartedOrStarting() {
        assert(!introspectorRPMService.isStartedOrStarting());
    }

    @Test
    public void testIsStoppedOrStopping() {
        assert(!introspectorRPMService.isStoppedOrStopping());
    }

    @Test
    public void testSendProfileData() {
        List<Long> result = introspectorRPMService.sendProfileData(new ArrayList<>());
        assertEquals(null, result);
    }

    @Test
    public void testIsConnected() {
        assert(!introspectorRPMService.isConnected());
    }

    @Test
    public void testGetHostString() {
        assertEquals(null, introspectorRPMService.getHostString());
    }

    @Test
    public void testGetApplicationName() {
        assertEquals("TestApp", introspectorRPMService.getApplicationName());
    }

    @Test
    public void testSendErrorData() {
        List<TracedError> tracedErrors = new ArrayList<>();
        introspectorRPMService.sendErrorData(tracedErrors);
    }

    @Test
    public void testSendSqlTraceData() {
        List<SqlTrace> sqlTraces = Collections.emptyList();
        introspectorRPMService.sendSqlTraceData(sqlTraces);
    }

    @Test
    public void testSendTransactionTraceData() {
        List<TransactionTrace> traces = Collections.emptyList();
        introspectorRPMService.sendTransactionTraceData(traces);
    }

    @Test
    public void testSendModules() {
        List<JarData> jarDataList = Collections.emptyList();
        introspectorRPMService.sendModules(jarDataList);
    }

    @Test
    public void testSendSpanEvents() {
        Collection<SpanEvent> events = Collections.emptyList();
        introspectorRPMService.sendSpanEvents(0, 0, events);
    }

    @Test
    public void testSendAnalyticsEvents() {
        Collection<TransactionEvent> events = Collections.emptyList();
        introspectorRPMService.sendAnalyticsEvents(0, 0, events);
    }

    @Test
    public void testSendCustomAnalyticsEvents() {
        Collection<CustomInsightsEvent> events = Collections.emptyList();
        introspectorRPMService.sendCustomAnalyticsEvents(0, 0, events);
    }

    @Test
    public void testSendErrorEvents() {
        Collection<ErrorEvent> events = Collections.emptyList();
        introspectorRPMService.sendErrorEvents(0, 0, events);
    }

    @Test
    public void testHarvestNow() {
        introspectorRPMService.harvestNow();
    }

    @Test
    public void testLaunch() {
        introspectorRPMService.launch();
    }

}
