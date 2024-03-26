/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "pekko", "scala" })
public class PekkoResponseWrapperTest {

    private static final long TIMEOUT = 30000;

    @Rule
    public HttpServerRule server = HttpServerRule$.MODULE$.apply(InstrumentationTestRunner.getIntrospector().getRandomPort(),
            new PekkoHttpTestRoutes().routes());

    @Test
    public void testAttributesContainsContentType() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first")
                .then()
                .body(containsString("prefix-first"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first"));

        String txName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(txName);
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        assertFalse(transactionEvents.isEmpty());
        TransactionEvent transactionEvent = (TransactionEvent) transactionEvents.toArray()[0];
        assertNotNull(transactionEvent);
        assertTrue(transactionEvent.getAttributes().containsKey("response.headers.contentType"));
        Object o = transactionEvent.getAttributes().get("response.headers.contentType");
        assertNotNull(o);
    }

    private String getTransactionPrefix() {
        return "WebTransaction/PekkoHttp";
    }
}
