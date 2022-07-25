/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.TransactionEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpanIdOnErrorsTest {
    private String APP_NAME;
    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = SpanParentTest.class.getClassLoader();

    private EnvironmentHolder holder;

    @Before
    public void before() throws Exception {
        holder = new EnvironmentHolder(new EnvironmentHolderSettingsGenerator(CONFIG_FILE, "all_enabled_test", CLASS_LOADER));
        holder.setupEnvironment();
        APP_NAME = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();

        cleanup();
    }

    @After
    public void after() {
        holder.close();
        cleanup();
    }

    private void cleanup() {
        ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        ServiceFactory.getRPMService().getErrorService().clearReservoir();
        Transaction.clearTransaction();
        ServiceFactory.getSpanEventService()
                .getOrCreateDistributedSamplingReservoir(APP_NAME)
                .clear();
        ServiceFactory.getTransactionEventsService()
                .getOrCreateDistributedSamplingReservoir(ServiceFactory.getRPMService().getApplicationName())
                .clear();
    }

    @Test
    public void addsRootSpanIdToErrorWhenErrorUnhandled() {
        runTransaction(new SpanErrorFlow.ThrowEscapingException());

        matchFirstErrorToOriginatingSpan("Custom/" + SpanErrorFlow.ThrowEscapingException.class.getName() + "/activeMethod");

        ensureSpanIdNotOnTransaction();
    }

    @Test
    public void addsNoticedSpanIdToError() {
        runTransaction(new SpanErrorFlow.NoticeErrorString());

        matchFirstErrorToOriginatingSpan("Custom/" + SpanErrorFlow.NoticeErrorString.class.getName() + "/activeMethod");

        ensureSpanIdNotOnTransaction();
    }

    @Test
    public void throwAndNoticeYieldsTheSpanWhereItWasNoticed() {
        runTransaction(new SpanErrorFlow.ThrowNoticeAndSquelch());

        matchFirstErrorToOriginatingSpan("Custom/" + SpanErrorFlow.ThrowNoticeAndSquelch.class.getName() + "/possiblyHandlingMethod");

        ensureSpanIdNotOnTransaction();
    }

    @Test
    public void noTransactionErrorIfErrorIsHandled() {
        runTransaction(new SpanErrorFlow.ThrowHandledException());

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        final DistributedSamplingPriorityQueue<ErrorEvent> reservoir =
                errorService.getReservoir(ServiceFactory.getRPMService().getApplicationName());

        assertEquals(0, reservoir.size());
    }

    private void matchFirstErrorToOriginatingSpan(String expectedSpanName) {
        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        final DistributedSamplingPriorityQueue<ErrorEvent> reservoir =
                errorService.getReservoir(ServiceFactory.getRPMService().getApplicationName());

        assertTrue(reservoir.size() > 0);
        final ErrorEvent errorEvent = reservoir.asList().get(0);
        assertTrue(errorEvent.getAgentAttributes().containsKey("spanId"));

        assertMethodWhereErrorOriginatedHasThisSpanId(errorEvent.getAgentAttributes().get("spanId"), expectedSpanName);
    }

    private void assertMethodWhereErrorOriginatedHasThisSpanId(Object spanId, String expectedSpanName) {
        List<String> seenSpanNames = new LinkedList<>();
        for(SpanEvent spanEvent : ServiceFactory.getSpanEventService().getOrCreateDistributedSamplingReservoir(APP_NAME).asList()) {
            if (spanEvent.getGuid().equals(spanId)) {
                assertEquals(expectedSpanName, spanEvent.getName());
                return;
            } else {
                seenSpanNames.add(spanEvent.getName() + " id " + spanEvent.getGuid());
            }
        }

        assertEquals(
                "Didn't find the span named " + expectedSpanName + " with id " + spanId,
                Collections.singletonList(expectedSpanName + " id " + spanId),
                seenSpanNames);
    }

    private void ensureSpanIdNotOnTransaction() {
        String appName = ServiceFactory.getRPMService().getApplicationName();
        final List<TransactionEvent> eventList = ServiceFactory.getTransactionEventsService()
                .getDistributedSamplingReservoir(appName).asList();
        assertEquals(1, eventList.size());
        assertFalse("spanId should not be on the transaction event.", eventList.get(0).getUserAttributesCopy().containsKey("spanId"));
    }

    private void runTransaction(SpanErrorFlow flow) {
        try {
            flow.transactionLaunchPoint();
        } catch (Throwable ignored) {
        }

        assertEquals(1, holder.getTransactionList().size());
    }

}
