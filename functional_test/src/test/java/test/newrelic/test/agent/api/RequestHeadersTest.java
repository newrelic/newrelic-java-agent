/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Trace;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RequestHeadersTest {
    @Test
    public void addingOutboundRequestHeadersDoesNotThrowInTracedMethodOutsideTransaction() {
        FakeHeaders fakeHeaders = new FakeHeaders();
        addHeaders(fakeHeaders);
        assertEquals("No headers should have been added.", 0, fakeHeaders.map.size());
    }

    /**
     * This method needs to start a TracedMethod and TransactionActivity, but
     * it should _not_ start a transaction.
     */
    @Trace(async = true)
    private void addHeaders(OutboundHeaders fakeHeaders) {
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(fakeHeaders);
    }

    private static class FakeHeaders implements OutboundHeaders {
        Map<String, String> map = new HashMap<>();

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public void setHeader(String name, String value) {
            map.put(name, value);
        }
    }
}
