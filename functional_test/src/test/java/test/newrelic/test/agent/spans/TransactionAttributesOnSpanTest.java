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
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TransactionAttributesOnSpanTest {
    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = TransactionAttributesOnSpanTest.class.getClassLoader();

    private EnvironmentHolder holder;
    private SamplingPriorityQueue<SpanEvent> spanEventPool;

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public String ymlEnvironmentName;

    @Parameterized.Parameter(2)
    public boolean isTransactionEventsEnabled;

    @Parameterized.Parameters(name = "{index}: Test with {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { "Span and Transaction events enabled", "transaction_events_enabled_attribute_filtering", true },
                { "Span enabled, Transaction events disabled", "transaction_events_disabled_attribute_filtering", false } };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        holder = new EnvironmentHolder(new EnvironmentHolderSettingsGenerator(CONFIG_FILE, ymlEnvironmentName, CLASS_LOADER));
        holder.setupEnvironment();
        String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        spanEventPool = ServiceFactory.getSpanEventService().getOrCreateDistributedSamplingReservoir(appName);
    }

    @After
    public void after() {
        holder.close();
        spanEventPool.clear();
    }

    @Test
    public void testTransactionEventsConfig() {
        if (isTransactionEventsEnabled) {
            assertTrue("Transaction Events enabled should be true but it is " + isTransactionEventsEnabled,
                    ServiceFactory.getTransactionEventsService().isEnabled());
        } else {
            assertFalse("Transaction Events enabled should be false but it is " + isTransactionEventsEnabled,
                    ServiceFactory.getTransactionEventsService().isEnabled());
        }
    }

    @Test
    public void testCustomParameterAndAttributeOnSpans() {
        String expectedSpanName = "Java/test.newrelic.test.agent.spans.TransactionAttributesOnSpanTest/transactionLaunchPoint";

        transactionLaunchPoint(1, 1);

        int numberOfSpansWithSpanAttribute = 0;
        String spanNameWithTransactionAttribute = null;
        List<SpanEvent> spanEvents = spanEventPool.asList();
        for (SpanEvent span : spanEvents) {
            if (span.getUserAttributesCopy().containsKey("txAttrib0")) {
                assertNull("Span, " + span.getName() + ", should not have a transaction attribute. The attribute " +
                        "is already on " + spanNameWithTransactionAttribute, spanNameWithTransactionAttribute);
                assertEquals(expectedSpanName, span.getName());
                spanNameWithTransactionAttribute = span.getName();
            }
            if (span.getUserAttributesCopy().containsKey("spanAttrib0")) {
                numberOfSpansWithSpanAttribute++;
            }
        }

        assertEquals(1, numberOfSpansWithSpanAttribute);
    }

    @Test
    public void testFilteringOfCustomParametersAndCustomAttributesOnSpans() {
        Map<String, String> expectedUserAttributesServiceEntrySpan = new HashMap<>();
        Map<String, String> expectedUserAttributesSecondSpan = new HashMap<>();

        expectedUserAttributesServiceEntrySpan.put("txAttrib0", "txValue0");
        expectedUserAttributesServiceEntrySpan.put("txAttrib1", "txValue1");
        expectedUserAttributesServiceEntrySpan.put("txAttrib3", "txValue3");
        expectedUserAttributesServiceEntrySpan.put("txAttrib4", "txValue4");

        expectedUserAttributesSecondSpan.put("spanAttrib0", "spanValue0");

        transactionLaunchPoint(6, 2);

        List<SpanEvent> spanEvents = spanEventPool.asList();
        for (SpanEvent span : spanEvents) {
            if (span.getIntrinsics().containsKey("nr.entryPoint")) {
                assertEquals(span.getUserAttributesCopy(), expectedUserAttributesServiceEntrySpan);
            } else {
                assertEquals(span.getUserAttributesCopy(), expectedUserAttributesSecondSpan);
            }
        }

    }

    @Trace(dispatcher = true)
    private void transactionLaunchPoint(int numberOfCustomParameters, int numberOfCustomAttributes) {
        for (int i = 0; i < numberOfCustomParameters; i++) {
            NewRelic.addCustomParameter("txAttrib" + i, "txValue" + i);
        }
        secondSpan(numberOfCustomAttributes);
    }

    @Trace
    private void secondSpan(int numberOfCustomAttributes) {
        for (int i = 0; i < numberOfCustomAttributes; i++) {
            NewRelic.getAgent().getTracedMethod().addCustomAttribute("spanAttrib" + i, "spanValue" + i);
        }
    }
}
