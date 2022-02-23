/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.google.common.collect.Iterables;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.TransactionStatsListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.*;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;

import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DistributedTracingTest {

    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = DistributedTracingTest.class.getClassLoader();

    public EnvironmentHolder setupEnvironemntHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Test
    public void testLateAcceptPayload() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            String payload = getAndCreateDistributedTracePayload();
            deepTransaction(payload);

            SpanEventsService spanEventsService = ServiceFactory.getServiceManager().getSpanEventsService();
            String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
            SamplingPriorityQueue<SpanEvent> spanEventsPool = spanEventsService.getOrCreateDistributedSamplingReservoir(appName);
            assertNotNull(spanEventsPool);
            List<SpanEvent> spanEvents = spanEventsPool.asList();
            assertNotNull(spanEvents);
            assertEquals(6, spanEvents.size());
            spanEventsPool.clear();

            SpanEvent firstSpanEvent = Iterables.getFirst(spanEvents, null);
            assertNotNull(firstSpanEvent);
            String traceId = firstSpanEvent.getTraceId();
            for (SpanEvent event : spanEvents) {
                // Assert that all tracers have the same traceId
                assertEquals(traceId, event.getTraceId());
            }
        } finally {
            holder.close();
        }
    }

    @Test
    public void testCrossApplicationTracingDisabled() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("cross_application_disabled_test");

        TransactionService transactionService = ServiceFactory.getTransactionService();
        final CountDownLatch latch = new CountDownLatch(1);
        TransactionStatsListener listener = new TransactionStatsListener() {
            @Override
            public void dispatcherTransactionStatsFinished(TransactionData transactionData, TransactionStats transactionStats) {
                // Use this to ensure that the transaction fully finished and that it
                // didn't bail out early (transaction stats listeners are fired at the end of tx processing)
                latch.countDown();
            }
        };

        try {
            transactionService.addTransactionStatsListener(listener);

            noCreateOrAcceptPayload();

            // Wait up to 30 seconds for the transaction to finish, if it doesn't then it means we encountered an issue and it never finished
            latch.await(30, TimeUnit.SECONDS);

            SpanEventsService spanEventsService = ServiceFactory.getServiceManager().getSpanEventsService();
            String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
            SamplingPriorityQueue<SpanEvent> spanEventsPool = spanEventsService.getOrCreateDistributedSamplingReservoir(appName);
            assertNotNull(spanEventsPool);
            List<SpanEvent> spanEvents = spanEventsPool.asList();
            assertNotNull(spanEvents);
            assertEquals(1, spanEvents.size());
            spanEventsPool.clear();

            SpanEvent firstSpanEvent = Iterables.getFirst(spanEvents, null);
            assertNotNull(firstSpanEvent);
            String traceId = firstSpanEvent.getTraceId();
            for (SpanEvent event : spanEvents) {
                // Assert that all tracers have the same traceId
                assertEquals(traceId, event.getTraceId());
            }

            TransactionDataList transactionList = holder.getTransactionList();
            assertNotNull(transactionList);
            assertEquals(1, transactionList.size());
        } finally {
            transactionService.removeTransactionStatsListener(listener);
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    public void noCreateOrAcceptPayload() {

    }

    @Trace(dispatcher = true)
    public String getAndCreateDistributedTracePayload() {
        DistributedTracePayload distributedTracePayload = AgentBridge.getAgent().getTransaction(false).createDistributedTracePayload();
        return distributedTracePayload.text();
    }

    @Trace(dispatcher = true)
    public void deepTransaction(final String payload) {
        NewRelic.setRequestAndResponse(new Request() {
            @Override
            public String getRequestURI() {
                return "/";
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public Enumeration getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String name) {
                return new String[0];
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public String getCookieValue(String name) {
                return null;
            }

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public String getHeader(String name) {
                if (name.equalsIgnoreCase("newrelic")) {
                    return payload;
                }
                return null;
            }
        }, null);
        secondLevel(payload);
    }

    @Trace
    public void secondLevel(String payload) {
        thirdLevel(payload);
    }

    @Trace
    public void thirdLevel(String payload) {
        fourthLevel(payload);
    }

    @Trace
    public void fourthLevel(String payload) {
        fifthLevel();
    }

    @Trace
    public void fifthLevel() {
        for (int i = 0; i < 100; i++) {
        }
    }

}
