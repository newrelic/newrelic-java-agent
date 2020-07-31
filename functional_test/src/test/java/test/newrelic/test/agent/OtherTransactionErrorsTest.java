/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;

public class OtherTransactionErrorsTest {

    static TransactionDataList transactions = new TransactionDataList();

    @BeforeClass
    public static void beforeClass() {
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
    }

    @Before
    public void before() {
        transactions.clear();
    }

    @Test
    public void testException() throws Exception {
        MockHttpRequest mockHttpRequest = new MockHttpRequest();
        mockHttpRequest.setMethod("GET");
        mockHttpRequest.setRequestURI("requesturi");
        mockHttpRequest.setHeader(AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME, "accept");
        mockHttpRequest.setHeader(AttributeNames.REQUEST_HOST_PARAMETER_NAME, "host");
        mockHttpRequest.setHeader(AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME, "useragent");
        mockHttpRequest.setHeader(AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME, "content-length");

        NewRelic.setRequestAndResponse(mockHttpRequest, new Response() {
            @Override
            public int getStatus() throws Exception {
                return 200;
            }

            @Override
            public String getStatusMessage() throws Exception {
                return "Status Message";
            }

            @Override
            public String getContentType() {
                return "text/content-type";
            }

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public void setHeader(String name, String value) {
                //
            }
        });

        try {
            new Runnable() {

                @Trace(dispatcher = true)
                @Override
                public void run() {
                    throw new RuntimeException();
                }
            }.run();
        } catch (RuntimeException ex) {
        }

        Assert.assertEquals(1, transactions.size());
        Assert.assertNotNull(transactions.get(0).getThrowable());
    }

    @Test
    public void testNoExceptionForNestedDispatcher() {
        new Runnable() {

            @Trace(dispatcher = true)
            @Override
            public void run() {
                try {
                    thisThrows();
                } catch (Exception ex) {
                }
            }

            @Trace(dispatcher = true)
            public void thisThrows() throws Exception {
                throw new Exception();
            }
        }.run();

        Assert.assertEquals(1, transactions.size());
        Assert.assertNull(transactions.get(0).getThrowable());
    }
}
