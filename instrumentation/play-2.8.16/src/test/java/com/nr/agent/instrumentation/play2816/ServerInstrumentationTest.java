/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play2816;

import java.util.Map;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;

import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java18IncompatibleTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({ Java17IncompatibleTest.class, Java18IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.play2816", "play" })
public class ServerInstrumentationTest {

    @Rule
    public PlayApplicationServerRule playApplicationServerRule = new PlayApplicationServerRule(InstrumentationTestRunner.getIntrospector().getRandomPort());

    @Test
    public void testControllerActions() {
        given()
                .baseUri("http://localhost:" + playApplicationServerRule.getTestServer().getRunningHttpPort())
                .when()
                .get("/hello")
                .then()
                .body(containsString("hello world"))
                .statusCode(200);
        assertTransactionNameWithMetrics(SimpleJavaController.class.getName(), "hello");

        InstrumentationTestRunner.getIntrospector().clear();

        given()
                .baseUri("http://localhost:" + playApplicationServerRule.getTestServer().getRunningHttpPort())
                .when()
                .get("/simple")
                .then()
                .body(containsString("Simple test"))
                .statusCode(200);
        assertTransactionNameWithMetrics(SimpleJavaController.class.getName(), "simple");

        InstrumentationTestRunner.getIntrospector().clear();

        given()
                .baseUri("http://localhost:" + playApplicationServerRule.getTestServer().getRunningHttpPort())
                .when()
                .get("/index")
                .then()
                .body(containsString("Ahoy ahoy"))
                .statusCode(200);
        assertTransactionNameWithMetrics(SimpleJavaController.class.getName(), "index");

        InstrumentationTestRunner.getIntrospector().clear();

        given()
                .baseUri("http://localhost:" + playApplicationServerRule.getTestServer().getRunningHttpPort())
                .when()
                .get("/scalaHello")
                .then()
                .body(containsString("Scala says hello world"))
                .statusCode(200);
        assertTransactionNameWithMetrics(SimpleScalaController.class.getName(), "scalaHello");

    }

    private void assertTransactionNameWithMetrics(String controllerName, String controllerMethod) {
        // This is not a WebTransaction because our Play instrumentation doesn't set a WebRequest or a
        // WebResponse; we instead rely on the Netty and/or Akka instrumentation to do that for us
        String expectedTxName = "OtherTransaction/PlayControllerAction/" +
                controllerName + "." + controllerMethod;

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000L);
        assertEquals(1, finishedTransactionCount);

        assertTrue(introspector.getTransactionNames().contains(expectedTxName));

        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(expectedTxName);
        assertTrue(metricsForTransaction.containsKey("Play2Routing"));
    }
}