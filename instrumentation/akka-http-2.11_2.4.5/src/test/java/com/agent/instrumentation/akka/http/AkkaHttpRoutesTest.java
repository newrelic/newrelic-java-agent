/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.akka.http;

import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.ValidatableResponse;
import com.newrelic.agent.HeadersUtil;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.test.marker.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

// Not compatible with Java 11+ and Scala 2.13+ https://github.com/scala/bug/issues/12340
@Category({ Java11IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"akka", "scala", "com.agent", "com.nr"})
public class AkkaHttpRoutesTest {

    private static final long TIMEOUT = 30000;

    @Rule
    public HttpServerRule server = HttpServerRule$.MODULE$.apply(InstrumentationTestRunner.getIntrospector().getRandomPort(),
            new AkkaHttpTestRoutes().routes());

    @Test
    public void testHostAndPort() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/hostandport")
                .then()
                .body(containsString("OK"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/hostandport"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testFutureErrorStatusCode() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/test-error")
                .then()
                .body(containsString("There was an internal server error."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/test-error"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "500");
    }

    @Test
    public void testFutureError2StatusCode() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/test-error-2")
                .then()
                .body(containsString("ErrorTest"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/test-error-2"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "500");
    }

    @Test
    public void testPrefixFirst() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first")
                .then()
                .body(containsString("prefix-first"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPrefixFirstFuture() {
        given().baseUri("http://localhost:" + server.getPort()).when()
                .get("/prefix-first-future")
                .then()
                .body(containsString("prefix-first-future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first-future"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPrefixFirstSecond() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first-second")
                .then()
                .body(containsString("prefix-first-second"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first-second"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPrefixFirstSecondFuture() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/prefix-first-second-future")
                .then()
                .body(containsString("prefix-first-second-future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first-second-future"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSimpleRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route")
                .then()
                .body(containsString("Simple Route"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSimpleRouteFuture() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/simple/route/future")
                .then()
                .body(containsString("Simple Route Future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route/future"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSimpleRouteWithQueryParam() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route?query=value")
                .then()
                .body(containsString("Simple Route"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSimpleRouteFutureWithQueryParam() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/simple/route/future?query=value")
                .then()
                .body(containsString("Simple Route Future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route/future"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSimpleRouteCAT() throws UnsupportedEncodingException {
        String idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead");

        ValidatableResponse response = given()
                .header(HeadersUtil.NEWRELIC_ID_HEADER, idHeader)
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/simple/route?query=value")
                .then()
                .body(containsString("Simple Route"));

        Headers responseHeaders = response.extract().headers();
        Assert.assertTrue(responseHeaders.hasHeaderWithName(HeadersUtil.NEWRELIC_APP_DATA_HEADER));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSimpleRouteFutureCAT() throws UnsupportedEncodingException {
        String idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead");

        ValidatableResponse response = given()
                .header(HeadersUtil.NEWRELIC_ID_HEADER, idHeader)
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/simple/route/future?query=value")
                .then()
                .body(containsString("Simple Route Future"));

        Headers responseHeaders = response.extract().headers();
        Assert.assertTrue(responseHeaders.hasHeaderWithName(HeadersUtil.NEWRELIC_APP_DATA_HEADER));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route/future"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testUUIDRoute() {
        String uuidRegex = "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}";
        UUID uuid = UUID.randomUUID();
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/uuid/" + uuid.toString())
                .then()
                .body(containsString("UUID: " + uuid.toString()));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/uuid/" + uuidRegex));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testUUIDFutureRoute() {
        String uuidRegex = "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}";
        UUID uuid = UUID.randomUUID();
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/uuid/future/" + uuid.toString())
                .then()
                .body(containsString("UUID Future: " + uuid.toString()));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/uuid/future/" + uuidRegex));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testRegexRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/regex/5")
                .then()
                .body(containsString("Regex: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/regex/\\d+"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testRegexFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/regex/future/5")
                .then()
                .body(containsString("Regex Future: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/regex/future/\\d+"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testNoMatchRegexRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/regex/a")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testNoMatchFutureRegexRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/regex/future/a")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testMapRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/red")
                .then()
                .body(containsString("Map: 1"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/red"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testMapFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/future/red")
                .then()
                .body(containsString("Map Future: 1"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/future/red"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternateMapRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/blue")
                .then()
                .body(containsString("Map: 3"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/blue"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternateMapFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/future/blue")
                .then()
                .body(containsString("Map Future: 3"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/future/blue"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/foobar")
                .then()
                .body(containsString("Segment: bar"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/foo~Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/segment/future/foobar")
                .then()
                .body(containsString("Segment Future: bar"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/future/foo~Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternateSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/food100")
                .then()
                .body(containsString("Segment: d100"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/foo~Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternateSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/segment/future/food100")
                .then()
                .body(containsString("Segment Future: d100"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/future/foo~Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testUnmatchedSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/segment")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testUnmatchedSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/segment/future")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testAlternateUnmatchedSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/segment/foo")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testAlternateUnmatchedSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/segment/future/foo")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testSecondaryAlternateUnmatchedSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/segment/foobar/baz")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testSecondaryAlternateUnmatchedSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/segment/future/foobar/baz")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testPathEndRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathend")
                .then()
                .body(containsString("PathEnd"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathend"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPathEndFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathendfuture")
                .then()
                .body(containsString("PathEndFuture"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathendfuture"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testNonPathEndRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathend/extra")
                .then()
                .body(containsString("PathEnd: extra"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathend/Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testNonPathEndFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/pathendfuture/extra")
                .then()
                .body(containsString("PathEndFuture: extra"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathendfuture/Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testRemainingPath() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/remaining")
                .then()
                .body(containsString("Remain: ing"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/remain~Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testRemainingFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futureremaining")
                .then()
                .body(containsString("FutureRemain: ing"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futureremain~Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternateRemainingRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/remaining/this/is/the/remaining")
                .then()
                .body(containsString("Remain: ing/this/is/the/remaining"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/remain~Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternateRestFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/futureremaining/this/is/the/remaining")
                .then()
                .body(containsString("FutureRemain: ing/this/is/the/remaining"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futureremain~Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSecondaryAlternateRestRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/remaining")
                .then()
                .body(containsString("Remain: ing"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/remain~Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSecondaryAlternateRemainingFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futureremaining")
                .then()
                .body(containsString("FutureRemain: ing"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futureremain~Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testRemainingPathRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/remainingpath/remaining")
                .then()
                .body(containsString("RemainingPath: remaining"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/remainingpath/RemainingPath"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testRemainingPathFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/futureremainingpath/remaining")
                .then()
                .body(containsString("FutureRemainingPath: remaining"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futureremainingpath/RemainingPath"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testIntNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/int/10")
                .then()
                .body(containsString("IntNumber: 10"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/int/IntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testIntNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/intfuture/10")
                .then()
                .body(containsString("IntNumberFuture: 10"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/intfuture/IntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testLongNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/long/1337")
                .then()
                .body(containsString("LongNumber: 1337"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/long/LongNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testLongNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/longfuture/1337")
                .then()
                .body(containsString("LongNumberFuture: 1337"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/longfuture/LongNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testHexIntNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/hexint/CAFE")
                .then()
                .body(containsString("HexIntNumber: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexint/HexIntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testHexIntNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/hexintfuture/CAFE")
                .then()
                .body(containsString("HexIntNumberFuture: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexintfuture/HexIntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testHexLongNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/hexlong/CAFE")
                .then()
                .body(containsString("HexLongNumber: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexlong/HexLongNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testHexLongNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/hexlongfuture/CAFE")
                .then()
                .body(containsString("HexLongNumberFuture: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexlongfuture/HexLongNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testDoubleNumberRoute() {
        String doubleRegex = "[+-]?\\d*\\.?\\d*";
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/double/123.456")
                .then()
                .body(containsString("DoubleNumber: 123.456"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/double/" + doubleRegex));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testDoubleNumberFutureRoute() {
        String doubleRegex = "[+-]?\\d*\\.?\\d*";
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/double/future/123.456")
                .then()
                .body(containsString("DoubleNumberFuture: 123.456"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/double/future/" + doubleRegex));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSegmentsRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/segments/here/are/segments")
                .then()
                .body(containsString("Segments: here,are,segments"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segments/(Segment/).repeat()"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testSegmentsFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/futuresegments/here/are/segments")
                .then()
                .body(containsString("FutureSegments: here,are,segments"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futuresegments/(Segment/).repeat()"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testUnmatchedRepeatRouteTooFewItems() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/repeat/52/complex")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testUnmatchedRepeatRouteTooManyItems() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/repeat/52/53/54/55/complex")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testComplexRepeatRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/repeat/52/53/54/complex")
                .then()
                .body(containsString("Repeat: 52,53,54"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/repeat/(IntNumber/).repeat()/complex"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testZeroRepeatRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/zerorepeat/")
                .then()
                .body(containsString("ZeroRepeat:"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/zerorepeat/().repeat()"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testComplexRepeatFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/futurerepeat/52/53/complex")
                .then()
                .body(containsString("FutureRepeat: 52,53"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futurerepeat/(IntNumber/).repeat()/complex"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativeComplexRepeatRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/repeat/52/53/54/complex")
                .then()
                .body(containsString("Repeat: 52,53,54"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/repeat/(IntNumber/).repeat()/complex"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativeComplexRepeatFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/futurerepeat/52/53/54/complex")
                .then()
                .body(containsString("FutureRepeat: 52,53,54"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futurerepeat/(IntNumber/).repeat()/complex"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPipeRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pipe/i5")
                .then()
                .body(containsString("Pipe: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pipe/i~IntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testFuturePipeRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurepipe/i5")
                .then()
                .body(containsString("FuturePipe: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurepipe/i~IntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativePipeRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pipe/hCAFE")
                .then()
                .body(containsString("Pipe: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pipe/h~HexIntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativePipeFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/futurepipe/hCAFE")
                .then()
                .body(containsString("FuturePipe: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurepipe/h~HexIntNumber"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPipeOptionalRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/pipe/optional/X/create")
                .then()
                .body(containsString("Pipe + Optional: null"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pipe/optional/X~IntNumber.?/create"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPipeOptionalFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/futurepipe/optional/X/create")
                .then()
                .body(containsString("FuturePipe + Optional: null"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurepipe/optional/X~IntNumber.?/create"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativePipeOptionalRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/pipe/optional/X71/edit")
                .then()
                .body(containsString("Pipe + Optional: 71"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pipe/optional/X~IntNumber.?/edit"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativePipeOptionalFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/futurepipe/optional/X71/edit")
                .then()
                .body(containsString("FuturePipe + Optional: 71"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurepipe/optional/X~IntNumber.?/edit"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testNegationRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/match")
                .then()
                .body(containsString("Negation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/match~!nomatch"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testNegationFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurematch")
                .then()
                .body(containsString("FutureNegation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurematch~!nomatch"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativeNegationRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/matchno")
                .then()
                .body(containsString("Negation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/match~!nomatch"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAlternativeNegationFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurematchno")
                .then()
                .body(containsString("FutureNegation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurematch~!nomatch"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testUnmatchedNegationRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/matchnomatch")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testUnmatchedNegationFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/futurematchnomatch")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testSinglePrefix() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/v1")
                .then()
                .body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testDoublePrefixNoParam() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/v1/containers")
                .then()
                .body(containsString("Request is missing required query parameter 'parameter'"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "404");
    }

    @Test
    public void testDoublePrefixWithParam() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/v1/containers?parameter=12345")
                .then()
                .body(containsString("ContainersParam: 12345"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/v1/containers"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testDoublePrefixWithSegment() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/v1/containers/DT12345")
                .then()
                .body(containsString("ContainersSegment: DT12345"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/v1/containers/Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testDoublePrefixWithSegmentAndString() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/v1/containers/DT12345/details")
                .then()
                .body(containsString("ContainersSegmentDetails: DT12345"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/v1/containers/Segment/details"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testDoublePrefixWithSegmentAndStrings() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when().get("/v1/containers/DT12345/details/test")
                .then()
                .body(containsString("ContainersSegmentDetailsTest: DT12345"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/v1/containers/Segment/details/test"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testCompleteWithFuture() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/future/2000")
                .then()
                .body(containsString("OK"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/future/IntNumber"));
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(getTransactionPrefix() + "/future/IntNumber");
        TransactionEvent transactionEvent = (TransactionEvent) transactionEvents.toArray()[0];
        Assert.assertNotNull(transactionEvent);
        Assert.assertTrue(transactionEvent.getDurationInSec() >= 2.0);

        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testCustomAsyncDirectiveNoResult() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/custom-directive/v2/docs?parameter=test")
                .then()
                .body(containsString("CustomDirectiveDocsParam: test"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/custom-directive/v2/docs"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");

    }

    @Test
    public void testCustomAsyncDirectiveWithResult() {
        for (int i = 0; i < 100; i++) {
            given()
                    .baseUri("http://localhost:" + server.getPort())
                    .header("X-NewRelic-Directive", "booyah!")
                    .when()
                    .get("/custom-directive/v2/docs/booyah")
                    .then()
                    .body(containsString("CustomDirectiveDocsSegment: booyah"));
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(100, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertEquals(1, introspector.getTransactionNames().size());
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/custom-directive/v2/docs/Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 100, "200");

    }

    @Test
    public void testPathEndRoute2() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/path-end")
                .then()
                .body(containsString("Get path end!"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/path-end"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPathEndRemainingRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/path-prefix-end")
                .then()
                .body(containsString("Get path end!"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/path-prefix-end"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPathEndRemainingRoute2() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/path-prefix-end/first-case")
                .then()
                .body(containsString("First case"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/path-prefix-end/first-case"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testPathEndRemainingRoute3() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/path-prefix-end/whatever")
                .then()
                .body(containsString("Remaining: whatever"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/path-prefix-end/Remaining"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
    }

    @Test
    public void testAsyncDirectiveRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort()).when()
                .get("/callid/102")
                .then()
                .body(containsString("Pong_OK"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Assert.assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/callid/Segment"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(introspector.getTransactionNames().toArray()[0].toString());
        assertResponseCodeOnTxEvents(transactionEvents, 1, "200");
        assertCustomAttributeOnTxEvents(transactionEvents, "attr1");
        assertCustomAttributeOnTxEvents(transactionEvents, "attr2");
    }

    private void assertResponseCodeOnTxEvents(Collection<TransactionEvent> transactionEvents, int expectedSize, String expectedResponseCode) {
        Assert.assertNotNull(transactionEvents);
        Assert.assertEquals(expectedSize, transactionEvents.size());
        for (TransactionEvent transactionEvent : transactionEvents) {
            String httpResponseCode = (String) transactionEvent.getAttributes().get("httpResponseCode");
            Assert.assertNotNull(httpResponseCode);
            Assert.assertEquals(expectedResponseCode, httpResponseCode);
            int statusCode = (Integer) transactionEvent.getAttributes().get("http.statusCode");
            Assert.assertEquals(Integer.parseInt(expectedResponseCode), statusCode);
        }
    }

    private void assertCustomAttributeOnTxEvents(Collection<TransactionEvent> transactionEvents, String expectedAttributeKey) {
        Assert.assertNotNull(transactionEvents);
        for (TransactionEvent transactionEvent : transactionEvents) {
            String attributeValue = String.valueOf(transactionEvent.getAttributes().get(expectedAttributeKey));
            Assert.assertNotNull(attributeValue);
        }
    }

    private String getTransactionPrefix() {
        return "WebTransaction/AkkaHttp";
    }

}
