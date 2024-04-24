/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.akka.http;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java22IncompatibleTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Collection;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

// Not compatible with Java 11+ and Scala 2.13+ https://github.com/scala/bug/issues/12340
@Category({ Java11IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class, Java22IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"akka", "scala"})
public class AkkaResponseWrapperTest {

    private static final long TIMEOUT = 30000;

    @Rule
    public HttpServerRule server = HttpServerRule$.MODULE$.apply(InstrumentationTestRunner.getIntrospector().getRandomPort(), new AkkaHttpTestRoutes().routes());

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
        TransactionEvent transactionEvent = (TransactionEvent)transactionEvents.toArray()[0];
        assertNotNull(transactionEvent);
        assertTrue(transactionEvent.getAttributes().containsKey("response.headers.contentType"));
        Object o = transactionEvent.getAttributes().get("response.headers.contentType");
        assertNotNull(o);
    }

    private String getTransactionPrefix() {
        return "WebTransaction/AkkaHttp";
    }
}
