/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import com.newrelic.api.agent.*;
import fi.iki.elonen.NanoHTTPD;
import org.junit.Assert;

/**
 * Embedded NanoHTTPD server for testing Messaging API
 */
public class MessagingTestServer extends NanoHTTPD {

    public MessagingTestServer(int port) {
        super(port);
    }

    @Override
    @Trace(dispatcher = true)
    public Response serve(IHTTPSession session) {
        Assert.assertNotNull(session.getHeaders().get("x-newrelic-id"));
        Assert.assertNotNull(session.getHeaders().get("x-newrelic-transaction"));
        Response response = newFixedLengthResponse("<html><body><h1>Successful Response</h1>\n</body></html>\n");

        // MessageConsumer/Listener
        NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageConsumeParameters
                .library("JMS")
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName("Message Destination")
                .inboundHeaders(new ApiTestHelper.CustomRequestWrapper(session, HeaderType.MESSAGE))
                .build());

        // send response headers, report external messaging produce with temp destination
        NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageProduceParameters
                .library("JMS")
                .destinationType(DestinationType.TEMP_QUEUE)
                .destinationName("Message Destination")
                .outboundHeaders(new ApiTestHelper.CustomResponseWrapper(response, HeaderType.MESSAGE))
                .build());

        Assert.assertNotNull(response.getHeader("NewRelicAppData"));

        return response;
    }
}
