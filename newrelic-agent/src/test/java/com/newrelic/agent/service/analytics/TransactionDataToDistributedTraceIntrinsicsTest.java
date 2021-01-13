/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.TransactionData;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.tracing.W3CTraceParent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionDataToDistributedTraceIntrinsicsTest {
    @Mock
    DistributedTracePayloadImpl distributedTracePayloadImpl;
    @Mock
    DistributedTraceService distributedTraceService;
    @Mock
    TransactionData transactionData;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(transactionData.getParentId()).thenReturn("myParent");
        when(transactionData.getParentSpanId()).thenReturn("myParentSpan");
        when(transactionData.getGuid()).thenReturn("spanguid");
        when(transactionData.getTripId()).thenReturn("tripid");
        when(transactionData.getTransportType()).thenReturn(TransportType.AMQP);
        when(transactionData.getTransportDurationInMillis()).thenReturn(1234L);
        when(transactionData.getLargestTransportDurationInMillis()).thenReturn(2345L);
        when(transactionData.getPriority()).thenReturn(1.5f);
    }

    @Test
    public void noInboundPayload() {
        mockServiceToReturnEmptyMap();

        Map<String, Object> result = constructTargetAndCall();

        assertEquals(Collections.<String, Object>emptyMap(), result);
        verify(distributedTraceService)
                .getIntrinsics(null, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, null, null, 1.5f);
    }

    @Test
    public void addsAttributesFromService() {
        when(distributedTraceService.getIntrinsics(any(DistributedTracePayloadImpl.class), anyString(), anyString(), any(TransportType.class), anyLong(),
                anyLong(), anyString(), anyString(), anyFloat()))
                .thenReturn(Collections.<String, Object>singletonMap("dt-service", "added-things"));

        when(distributedTracePayloadImpl.getGuid()).thenReturn("inbound-guid");
        when(distributedTracePayloadImpl.getTransactionId()).thenReturn("inbound-transaction-id");
        when(transactionData.getInboundDistributedTracePayload()).thenReturn(distributedTracePayloadImpl);

        Map<String, Object> result = constructTargetAndCall();

        assertEquals(ImmutableMap.<String, Object>of("parentSpanId", "myParentSpan", "parentId", "myParent", "dt-service", "added-things"), result);

        verify(distributedTraceService)
                .getIntrinsics(distributedTracePayloadImpl, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, "myParent", "myParentSpan", 1.5f);
    }

    @Test
    public void avoidsParentingAttributesIfToldNotTo() {
        when(distributedTraceService.getIntrinsics(any(DistributedTracePayloadImpl.class), anyString(), anyString(), any(TransportType.class), anyLong(),
                anyLong(), anyString(), anyString(), anyFloat()))
                .thenReturn(Collections.<String, Object>singletonMap("dt-service", "added-things"));

        when(distributedTracePayloadImpl.getGuid()).thenReturn("inbound-guid");
        when(distributedTracePayloadImpl.getTransactionId()).thenReturn("inbound-transaction-id");
        when(transactionData.getInboundDistributedTracePayload()).thenReturn(distributedTracePayloadImpl);

        TransactionDataToDistributedTraceIntrinsics target = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        Map<String, Object> result = target.buildDistributedTracingIntrinsics(transactionData, false);

        assertEquals(ImmutableMap.<String, Object>of("dt-service", "added-things"), result);

        verify(distributedTraceService)
                .getIntrinsics(distributedTracePayloadImpl, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, "myParent", "myParentSpan", 1.5f);
    }

    @Test
    public void inboundPayloadWithParentTransactionAndSpan() {
        mockServiceToReturnEmptyMap();

        when(distributedTracePayloadImpl.getGuid()).thenReturn("inbound-guid");
        when(distributedTracePayloadImpl.getTransactionId()).thenReturn("inbound-transaction-id");
        when(transactionData.getInboundDistributedTracePayload()).thenReturn(distributedTracePayloadImpl);

        Map<String, Object> result = constructTargetAndCall();

        assertEquals(ImmutableMap.<String, Object>of("parentSpanId", "myParentSpan", "parentId", "myParent"), result);

        verify(distributedTraceService)
                .getIntrinsics(distributedTracePayloadImpl, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, "myParent", "myParentSpan", 1.5f);
    }

    @Test
    public void inboundPayloadWithParentTransactionButNoSpan() {
        mockServiceToReturnEmptyMap();

        when(distributedTracePayloadImpl.getTransactionId()).thenReturn("inbound-transaction-id");
        when(transactionData.getInboundDistributedTracePayload()).thenReturn(distributedTracePayloadImpl);

        Map<String, Object> result = constructTargetAndCall();

        assertEquals(ImmutableMap.<String, Object>of("parentId", "myParent"), result);

        verify(distributedTraceService)
                .getIntrinsics(distributedTracePayloadImpl, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, "myParent", null, 1.5f);
    }

    @Test
    public void inboundPayloadWithParentSpanButNoTransaction() {
        mockServiceToReturnEmptyMap();

        when(distributedTracePayloadImpl.getGuid()).thenReturn("inbound-guid");
        when(transactionData.getInboundDistributedTracePayload()).thenReturn(distributedTracePayloadImpl);

        Map<String, Object> result = constructTargetAndCall();

        assertEquals(ImmutableMap.<String, Object>of("parentSpanId", "myParentSpan"), result);

        verify(distributedTraceService)
                .getIntrinsics(distributedTracePayloadImpl, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, null, "myParentSpan", 1.5f);
    }

    @Test
    public void inboundPayloadWithW3CParent() {
        mockServiceToReturnEmptyMap();

        when(transactionData.getW3CTraceParent()).thenReturn(mock(W3CTraceParent.class));
        when(transactionData.getInboundDistributedTracePayload()).thenReturn(distributedTracePayloadImpl);

        Map<String, Object> result = constructTargetAndCall();

        assertEquals(ImmutableMap.<String, Object>of("parentSpanId", "myParentSpan"), result);

        verify(distributedTraceService)
                .getIntrinsics(distributedTracePayloadImpl, "spanguid", "tripid", TransportType.AMQP, 1234L, 2345L, null, "myParentSpan", 1.5f);
    }

    private Map<String, Object> constructTargetAndCall() {
        TransactionDataToDistributedTraceIntrinsics target = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        return target.buildDistributedTracingIntrinsics(transactionData, true);
    }

    private void mockServiceToReturnEmptyMap() {
        when(distributedTraceService.getIntrinsics(any(DistributedTracePayloadImpl.class), anyString(), anyString(), any(TransportType.class), anyLong(),
                anyLong(), anyString(), anyString(), anyFloat()))
                .thenReturn(Collections.<String, Object>emptyMap());
    }
}