/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

/**
 * Transaction (CAT and Synthetics) Headers tests.
 *
 * InboundHeaders can be provided from three different sources: instrumentation, CAT API, and dispatcher Request.
 *
 * Instrumentation and Request headers are obfuscated. CAT API provided headers are not.
 */
@Category(RequiresFork.class)
public class TransactionInboundHeadersTest {

    @Before
    public void setup() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @Test
    public void testNullHeaders() {
        Transaction tx = Transaction.getTransaction();
        tx.provideHeaders(null);
        InboundHeaderState inboundHeaderState = tx.getInboundHeaderState();

        Assert.assertTrue(inboundHeaderState != null);
        Assert.assertNull(inboundHeaderState.getClientCrossProcessId());
        Assert.assertNull(inboundHeaderState.getUnparsedSyntheticsHeader());

        // Unknown content length should be -1.
        Assert.assertEquals(-1, inboundHeaderState.getRequestContentLength());
    }

    @Test
    public void testEmptyInstrumentationProvidedHeaders() {
        Transaction tx = Transaction.getTransaction();
        InboundHeaders headers = createEmptyHTTPInboundHeaders();
        tx.provideHeaders(headers);

        InboundHeaderState inboundHeaderState = tx.getInboundHeaderState();
        Assert.assertTrue(inboundHeaderState != null);
        Assert.assertNull(inboundHeaderState.getClientCrossProcessId());
        Assert.assertNull(inboundHeaderState.getUnparsedSyntheticsHeader());
        Assert.assertNull(inboundHeaderState.getReferrerGuid());
    }

    @Test
    public void testEmptyCatApiProvidedHeaders() {
        Transaction tx = Transaction.getTransaction();
        InboundHeaders headers = createEmptyMessageInboundHeaders();
        tx.provideRawHeaders(headers);

        InboundHeaderState inboundHeaderState = tx.getInboundHeaderState();
        Assert.assertTrue(inboundHeaderState != null);
        Assert.assertNull(inboundHeaderState.getClientCrossProcessId());
        Assert.assertNull(inboundHeaderState.getUnparsedSyntheticsHeader());
        Assert.assertNull(inboundHeaderState.getReferrerGuid());
    }

    @Test
    public void testNullDispatcher() {
        Transaction tx = Transaction.getTransaction();
        Dispatcher dispatcher = Mockito.mock(Dispatcher.class);
        Mockito.when(dispatcher.getRequest()).thenReturn(null);
        tx.setDispatcher(dispatcher);

        InboundHeaderState inboundHeaderState = tx.getInboundHeaderState();
        Assert.assertTrue(inboundHeaderState != null);
        Assert.assertNull(inboundHeaderState.getClientCrossProcessId());
        Assert.assertNull(inboundHeaderState.getUnparsedSyntheticsHeader());
        Assert.assertNull(inboundHeaderState.getReferrerGuid());
    }

    @Test
    public void TestNullRequestRequestHeaders() {
        Transaction tx = Transaction.getTransaction();
        Dispatcher dispatcher = Mockito.mock(Dispatcher.class);
        Mockito.when(dispatcher.getRequest()).thenReturn(null);
        tx.setDispatcher(dispatcher);

        InboundHeaderState inboundHeaderState = tx.getInboundHeaderState();
        Assert.assertTrue(inboundHeaderState != null);
        Assert.assertNull(inboundHeaderState.getClientCrossProcessId());
        Assert.assertNull(inboundHeaderState.getUnparsedSyntheticsHeader());
        Assert.assertNull(inboundHeaderState.getReferrerGuid());

        InboundHeaders requestHeaders = Transaction.getRequestHeaders(tx);
        Assert.assertNull(requestHeaders);
    }

    @Test
    public void TestEmptyRequestHeaders() {
        Transaction tx = Transaction.getTransaction();
        Dispatcher dispatcher = Mockito.mock(Dispatcher.class);
        Request request = Mockito.mock(Request.class);
        Mockito.when(dispatcher.getRequest()).thenReturn(request);
        tx.setDispatcher(dispatcher);

        InboundHeaders requestHeaders = Transaction.getRequestHeaders(tx);
        // Non-null request headers should be deobfuscated.
        Assert.assertEquals(requestHeaders.getClass(), DeobfuscatedInboundHeaders.class);
    }

    /**
     * Utility methods.
     */
    private InboundHeaders createEmptyHTTPInboundHeaders() {
        return new InboundHeaders() {

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public String getHeader(String key) {
                return null;
            }
        };

    }

    private InboundHeaders createEmptyMessageInboundHeaders() {
        return new InboundHeaders() {

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.MESSAGE;
            }

            @Override
            public String getHeader(String key) {
                return null;
            }
        };
    }
}
