/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RootSpanStatusErrorsTest {
    private String APP_NAME;
    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = RootSpanStatusErrorsTest.class.getClassLoader();

    private EnvironmentHolder holder;

    @Before
    public void before() throws Exception {
        holder = new EnvironmentHolder(new EnvironmentHolderSettingsGenerator(CONFIG_FILE, "all_enabled_test", CLASS_LOADER));
        holder.setupEnvironment();
        APP_NAME = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();

        ServiceFactory.getSpanEventService()
                .getOrCreateDistributedSamplingReservoir(APP_NAME)
                .clear();
    }

    @After
    public void after() {
        holder.close();

        ServiceFactory.getSpanEventService()
                .getOrCreateDistributedSamplingReservoir(APP_NAME)
                .clear();
    }

    @Test
    public void onlyUnhandledExceptionShouldEndUpOnRootSpan() {
        runTransaction(500, new SpanErrorFlow.ThrowEscapingException());

        assertRootSpanAttributes(null, RuntimeException.class.getName());
    }

    @Test
    public void statusCodeShouldOnlyBeStringOnErrorClassOnRootSpan() {
        runTransaction(414, new SpanErrorFlow.ThrowHandledException());

        assertRootSpanAttributes(null, "414");
    }

    private void assertRootSpanAttributes(Integer expectedStatus, String errorClass) {
        SamplingPriorityQueue<SpanEvent> reservoir = ServiceFactory.getSpanEventService()
                .getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertTrue(reservoir.asList().size() > 0);
        List<String> seenNames = new LinkedList<>();
        for(SpanEvent event : reservoir.asList()) {
            if (!event.getName().startsWith("Java/")) {
                seenNames.add(event.getName());
                continue;
            }

            assertEquals(
                    "wrong error.status attribute",
                    expectedStatus,
                    event.getAgentAttributes().get("error.status")
            );

            assertEquals(
                    "wrong error.class attribute",
                    errorClass,
                    event.getAgentAttributes().get("error.class")
            );

            return;
        }

        assertEquals("Did not find root span!", Collections.emptyList(), seenNames);
    }


    private void runTransaction(int statusCode, SpanErrorFlow tracedCode) {
        try {
            tracedCode.webLaunchPoint(statusCode);
        } catch (Throwable ignored) {
        }

        assertEquals(1, holder.getTransactionList().size());
    }

}
