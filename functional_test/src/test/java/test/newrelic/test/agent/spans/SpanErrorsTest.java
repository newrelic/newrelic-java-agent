/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SpanErrorsTest {
    private String APP_NAME;
    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = SpanErrorsTest.class.getClassLoader();

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
    public void testNoErrorsNoAttributes() {
        runTransaction(new SpanErrorFlow.Nothing());

        checkSpansNoOtherAttributes(Collections.<String, List<String>>emptyMap());
    }

    @Test
    public void testErrorsOnAllSpansUnhandled() {
        runTransaction(new SpanErrorFlow.ThrowEscapingException());

        checkSpans(
                Collections.<String, List<String>>emptyMap(),
                Arrays.asList("~~ oops ~~", RuntimeException.class.getName()));
    }

    @Test
    public void testErrorsOnSomeSpansHandled() {
        runTransaction(new SpanErrorFlow.ThrowHandledException());

        checkSpansNoOtherAttributes(ImmutableMap.of(
                "Custom/" + SpanErrorFlow.ThrowHandledException.class.getName() + "/activeMethod",
                Arrays.asList("~~ oops ~~", RuntimeException.class.getName()),
                "Custom/" + SpanErrorFlow.ThrowHandledException.class.getName() + "/intermediatePassThroughMethod",
                Arrays.asList("~~ oops ~~", RuntimeException.class.getName())
        ));
    }

    @Test
    public void testNoticedExceptionDoesNotBubble() {
        runTransaction(new SpanErrorFlow.NoticeErrorException());

        checkSpansNoOtherAttributes(Collections.singletonMap(
                "Custom/" + SpanErrorFlow.NoticeErrorException.class.getName() + "/activeMethod",
                Arrays.asList("~~ noticed ~~", Exception.class.getName())
        ));
    }

    @Test
    public void testNoticedStringDoesNotBubble() {
        runTransaction(new SpanErrorFlow.NoticeErrorString());

        checkSpansNoOtherAttributes(Collections.singletonMap(
                "Custom/" + SpanErrorFlow.NoticeErrorString.class.getName() + "/activeMethod", Arrays.asList("~~ noticed string ~~", null)
        ));
    }

    @Test
    public void testNoticedErrorOverridesThrownError() {
        runTransaction(new SpanErrorFlow.NoticeAndThrow());

        checkSpans(
                Collections.singletonMap(
                        "Custom/" + SpanErrorFlow.NoticeAndThrow.class.getName() + "/activeMethod",
                        Arrays.asList("noticed, not thrown", null)
                ),
                Arrays.asList("/ by zero", ArithmeticException.class.getName())
        );
    }

    @Test
    public void testCatchAndRethrowCapturesLastException() {
        runTransaction(new SpanErrorFlow.CatchAndRethrow());

        checkSpans(
                ImmutableMap.of(
                        "Custom/" + SpanErrorFlow.CatchAndRethrow.class.getName() + "/activeMethod",
                        Arrays.asList("~~ oops ~~", RuntimeException.class.getName()),
                        "Custom/" + SpanErrorFlow.CatchAndRethrow.class.getName() + "/intermediatePassThroughMethod",
                        Arrays.asList("~~ oops ~~", RuntimeException.class.getName())
                ),
                Arrays.asList("~~ caught ~~", CustomFooException.class.getName())
        );
    }

    private void checkSpansNoOtherAttributes(Map<String, List<String>> spanChecks) {
        checkSpans(spanChecks, Arrays.<String>asList(null, null));
    }

    private void checkSpans(Map<String, List<String>> spanChecks, List<String> defaultChecks) {
        SamplingPriorityQueue<SpanEvent> reservoir = ServiceFactory.getSpanEventService()
                .getOrCreateDistributedSamplingReservoir(APP_NAME);

        assertTrue(reservoir.asList().size() > 0);
        for (SpanEvent event : reservoir.asList()) {
            if (spanChecks.containsKey(event.getName())) {
                assertEquals(
                        "wrong error.message for " + event.getName(),
                        spanChecks.get(event.getName()).get(0),
                        event.getAgentAttributes().get("error.message"));
                assertEquals(
                        "wrong error.class for " + event.getName(),
                        spanChecks.get(event.getName()).get(1),
                        event.getAgentAttributes().get("error.class"));
            } else {
                assertEquals(
                        "wrong error.message for " + event.getName(),
                        defaultChecks.get(0),
                        event.getAgentAttributes().get("error.message"));
                assertEquals(
                        "wrong error.class for " + event.getName(),
                        defaultChecks.get(1),
                        event.getAgentAttributes().get("error.class"));
            }
            assertNull(
                    "error.status should never be on " + event.getName(),
                    event.getAgentAttributes().get("error.status"));
        }
    }

    private void runTransaction(SpanErrorFlow tracedCode) {
        try {
            tracedCode.transactionLaunchPoint();
        } catch (Throwable ignored) {
        }

        assertEquals(1, holder.getTransactionList().size());
    }

}