/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TraceAccessorApiTest {

    private static final String CONFIG_FILE = "configs/trace_accessor_test.yml";
    private static final ClassLoader CLASS_LOADER = TraceAccessorApiTest.class.getClassLoader();

    public EnvironmentHolder setupEnvironemntHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Test
    public void testDistributedTracingDisabledGetTraceId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("dt_disabled_test");

        try {
            startTxWithDtDisabledAndAssertEmptyTraceId();
            harvestAndCheckTxn(holder);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testDistributedTracingEnabledGetTraceId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            startTxAndAssertTraceIdEqualsTransactionGuid();
            harvestAndCheckTxn(holder);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testAcceptPayloadGetTraceId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            DistributedTracePayloadImpl distributedTracePayload = getDistributedTracePayload();
            // We don't care about the DT payload transaction, so we will harvest and clear the tx list before continuing
            harvestAndCheckTxn(holder);
            TransactionDataList transactionList = holder.getTransactionList();
            transactionList.clear();

            startTxAcceptPayloadAndAssertTraceIdDoesNotEqualTransactionGuid(distributedTracePayload);
            harvestAndCheckTxn(holder);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testCreatePayloadGetTraceId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            startTxCreatePayloadAndAssertTraceIdEqualsTransactionGuid();
            harvestAndCheckTxn(holder);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testNoTransactionGetTraceId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();
            assertEquals("", traceId);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testSegmentGetTraceId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            startSegmentAndAssertTraceId();
            harvestAndCheckTxn(holder);
        } finally {
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    private void startSegmentAndAssertTraceId() {
        Segment testSegment = NewRelic.getAgent().getTransaction().startSegment("TestSegment");
        String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();

        Transaction txn = ServiceFactory.getServiceManager().getTransactionService().getTransaction(false);
        assertEquals(txn.getSpanProxy().getTraceId(), traceId); // A segment should not affect the traceId
        testSegment.end();
    }

    @Trace(dispatcher = true)
    private void startTxWithDtDisabledAndAssertEmptyTraceId() {
        String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();
        assertEquals("", traceId);
    }

    @Trace(dispatcher = true)
    private void startTxAndAssertTraceIdEqualsTransactionGuid() {
        Transaction txn = ServiceFactory.getServiceManager().getTransactionService().getTransaction(false);
        String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();
        assertEquals(txn.getSpanProxy().getTraceId(), traceId);
    }

    @Trace(dispatcher = true)
    private DistributedTracePayloadImpl getDistributedTracePayload() {
        return (DistributedTracePayloadImpl) NewRelic.getAgent().getTransaction().createDistributedTracePayload();
    }

    @Trace(dispatcher = true)
    private void startTxAcceptPayloadAndAssertTraceIdDoesNotEqualTransactionGuid(DistributedTracePayloadImpl distributedTracePayload) {
        Transaction txn = ServiceFactory.getServiceManager().getTransactionService().getTransaction(false);
        NewRelic.getAgent().getTransaction().acceptDistributedTracePayload(distributedTracePayload);
        String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();
        assertNotEquals(txn.getGuid(), traceId); // Accepting a payload should change the traceId so it won't match the original tx guid
        assertEquals(distributedTracePayload.traceId, traceId);
    }

    @Trace(dispatcher = true)
    private void startTxCreatePayloadAndAssertTraceIdEqualsTransactionGuid() {
        DistributedTracePayloadImpl distributedTracePayload = (DistributedTracePayloadImpl) NewRelic.getAgent()
                .getTransaction()
                .createDistributedTracePayload();
        String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();
        assertEquals(distributedTracePayload.traceId, traceId); // Creating a payload should not change the traceId
    }

    @Test
    public void testDistributedTracingDisabledGetSpanId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("dt_disabled_test");

        try {
            startTxAndAssertEmptySpanId();
            harvestAndCheckTxn(holder);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testDistributedTracingEnabledGetSpanId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            String spanId = startTxAndReturnSpanId();
            harvestAndCheckTxn(holder);
            TransactionDataList transactionList = holder.getTransactionList();
            Tracer rootTracer = transactionList.get(0).getRootTracer();
            String expectedGuid = rootTracer.getGuid();
            assertEquals(expectedGuid, spanId);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testAcceptPayloadGetSpanId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            DistributedTracePayloadImpl distributedTracePayload = getDistributedTracePayload();
            // We don't care about the DT payload transaction, so we will harvest and clear the tx list before continuing
            harvestAndCheckTxn(holder);
            TransactionDataList transactionList = holder.getTransactionList();
            transactionList.clear();

            String spanId = startTxAcceptPayloadAndReturnSpanId(distributedTracePayload);
            harvestAndCheckTxn(holder);
            Tracer rootTracer = transactionList.get(0).getRootTracer();
            String expectedGuid = rootTracer.getGuid();
            assertEquals(expectedGuid, spanId);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testCreatePayloadGetSpanId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            String spanId = startTxCreatePayloadAndReturnSpanId();
            harvestAndCheckTxn(holder);
            TransactionDataList transactionList = holder.getTransactionList();
            Tracer rootTracer = transactionList.get(0).getRootTracer();
            String expectedGuid = rootTracer.getGuid();
            assertEquals(expectedGuid, spanId);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testNoTransactionGetSpanId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            String spanId = AgentBridge.getAgent().getTraceMetadata().getSpanId();
            assertEquals("", spanId);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testSegmentGetSpanId() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");

        try {
            String spanId = startSegmentAndReturnSpanId();

            harvestAndCheckTxn(holder);
            TransactionDataList transactionList = holder.getTransactionList();
            Tracer rootTracer = transactionList.get(0).getRootTracer();
            String expectedGuid = rootTracer.getGuid();
            assertEquals(expectedGuid, spanId);
        } finally {
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    private String startSegmentAndReturnSpanId() {
        Segment testSegment = NewRelic.getAgent().getTransaction().startSegment("TestSegment");

        // A segment should not affect the spanId
        String spanId = AgentBridge.getAgent().getTraceMetadata().getSpanId();
        testSegment.end();

        return spanId;
    }

    @Trace(dispatcher = true)
    private void startTxAndAssertEmptySpanId() {
        String traceId = AgentBridge.getAgent().getTraceMetadata().getTraceId();
        assertEquals("", traceId);
    }

    @Trace(dispatcher = true)
    private String startTxAndReturnSpanId() {
        return AgentBridge.getAgent().getTraceMetadata().getSpanId();
    }

    @Trace(dispatcher = true)
    private String startTxAcceptPayloadAndReturnSpanId(DistributedTracePayloadImpl distributedTracePayload) {
        // Accepting a payload should not change the spanId
        NewRelic.getAgent().getTransaction().acceptDistributedTracePayload(distributedTracePayload);
        return AgentBridge.getAgent().getTraceMetadata().getSpanId();
    }

    @Trace(dispatcher = true)
    private String startTxCreatePayloadAndReturnSpanId() {
        // Creating a payload should not change the spanId
        NewRelic.getAgent()
                .getTransaction()
                .createDistributedTracePayload();
        return AgentBridge.getAgent().getTraceMetadata().getSpanId();
    }

    private void harvestAndCheckTxn(EnvironmentHolder holder) throws InterruptedException {
        TransactionDataList transactionList = holder.getTransactionList();
        ServiceFactory.getHarvestService().harvestNow();
        transactionList.waitFor(1, TimeUnit.SECONDS.toMillis(30));
        assertEquals(1, transactionList.size());
    }

}
