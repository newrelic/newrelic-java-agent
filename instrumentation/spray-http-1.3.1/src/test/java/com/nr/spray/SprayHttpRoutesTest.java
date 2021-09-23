/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.spray;

import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.ValidatableResponse;
import com.newrelic.agent.HeadersUtil;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.test.marker.Java17IncompatibleTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Category({Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"spray", "akka", "scala"})
public class SprayHttpRoutesTest {

    private static int DEFAULT_TIMEOUT_MILLIS = 1200;

    @Rule
    public HttpServerRule server = HttpServerRule$.MODULE$.apply(InstrumentationTestRunner.getIntrospector().getRandomPort(), SprayHttpTestRoutes.routes());

    @Test
    public void testPrefixFirst() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first")
                .then()
                .body(containsString("prefix-first"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first"));
    }

    @Test
    public void testPrefixFirstFuture() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first-future").then()
                .body(containsString("prefix-first-future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first-future"));
    }

    @Test
    public void testPrefixFirstSecond() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first-second").then()
                .body(containsString("prefix-first-second"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first-second"));
    }

    @Test
    public void testPrefixFirstSecondFuture() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first-second-future")
                .then()
                .body(containsString("prefix-first-second-future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/prefix-first-second-future"));
    }

    @Test
    public void testSimpleRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route").then()
                .body(containsString("Simple Route"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route"));
    }

    @Test
    public void testSimpleRouteFuture() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route/future").then()
                .body(containsString("Simple Route Future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route/future"));
    }

    @Test
    public void testSimpleRouteWithQueryParam() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route?query=value").then()
                .body(containsString("Simple Route"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route"));
    }

    @Test
    public void testSimpleRouteFutureWithQueryParam() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route/future?query=value").then()
                .body(containsString("Simple Route Future"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route/future"));
    }

    @Test
    public void testSimpleRouteCAT() throws UnsupportedEncodingException {
        String idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead");

        ValidatableResponse response = given()
                .header(HeadersUtil.NEWRELIC_ID_HEADER, idHeader)
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route?query=value")
                .then()
                .body(containsString("Simple Route"));

        Headers responseHeaders = response.extract().headers();
        Assert.assertTrue(responseHeaders.hasHeaderWithName(HeadersUtil.NEWRELIC_APP_DATA_HEADER));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route"));
    }

    @Test
    public void testSimpleRouteFutureCAT() throws UnsupportedEncodingException {
        String idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead");

        ValidatableResponse response = given()
                .header(HeadersUtil.NEWRELIC_ID_HEADER, idHeader)
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/simple/route/future?query=value")
                .then()
                .body(containsString("Simple Route Future"));

        Headers responseHeaders = response.extract().headers();
        Assert.assertTrue(responseHeaders.hasHeaderWithName(HeadersUtil.NEWRELIC_APP_DATA_HEADER));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/simple/route/future"));
    }

    @Test
    public void testUUIDRoute() {
        String uuidRegex = "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}";
        UUID uuid = UUID.randomUUID();
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/uuid/" + uuid.toString()).then()
                .body(containsString("UUID: " + uuid.toString()));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/uuid/" + uuidRegex));
    }

    @Test
    public void testUUIDFutureRoute() {
        String uuidRegex = "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}";
        UUID uuid = UUID.randomUUID();
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/uuid/future/" + uuid.toString())
                .then().body(containsString("UUID Future: " + uuid.toString()));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/uuid/future/" + uuidRegex));
    }

    @Test
    public void testRegexRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/regex/5").then()
                .body(containsString("Regex: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/regex/\\d+"));
    }

    @Test
    public void testRegexFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/regex/future/5").then()
                .body(containsString("Regex Future: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/regex/future/\\d+"));
    }

    @Test
    public void testNoMatchRegexRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/regex/a")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testNoMatchFutureRegexRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/regex/future/a")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testMapRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/red").then()
                .body(containsString("Map: 1"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/red"));
    }

    @Test
    public void testMapFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/future/red").then()
                .body(containsString("Map Future: 1"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/future/red"));
    }

    @Test
    public void testAlternateMapRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/blue").then()
                .body(containsString("Map: 3"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/blue"));
    }

    @Test
    public void testAlternateMapFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/map/future/blue").then()
                .body(containsString("Map Future: 3"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/map/future/blue"));
    }

    @Test
    public void testSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/foobar").then()
                .body(containsString("Segment: bar"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/foo~Segment"));
    }

    @Test
    public void testSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/future/foobar").then()
                .body(containsString("Segment Future: bar"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/future/foo~Segment"));
    }

    @Test
    public void testAlternateSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/food100").then()
                .body(containsString("Segment: d100"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/foo~Segment"));
    }

    @Test
    public void testAlternateSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/future/food100").then()
                .body(containsString("Segment Future: d100"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segment/future/foo~Segment"));
    }

    @Test
    public void testUnmatchedSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testUnmatchedSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/future")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testAlternateUnmatchedSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/foo")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testAlternateUnmatchedSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/future/foo")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testSecondaryAlternateUnmatchedSegmentRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/foobar/baz")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testSecondaryAlternateUnmatchedSegmentFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segment/future/foobar/baz")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testPathEndRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathend").then()
                .body(containsString("PathEnd"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathend"));
    }

    @Test
    public void testPathEndFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathendfuture").then()
                .body(containsString("PathEndFuture"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathendfuture"));
    }

    @Test
    public void testNonPathEndRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathend/extra").then()
                .body(containsString("PathEnd: extra"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathend/Segment"));
    }

    @Test
    public void testNonPathEndFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pathendfuture/extra").then()
                .body(containsString("PathEndFuture: extra"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/pathendfuture/Segment"));
    }

    @Test
    public void testRestRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/resting").then()
                .body(containsString("Rest: ing"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/rest~Rest"));
    }

    @Test
    public void testRestFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futureresting").then()
                .body(containsString("FutureRest: ing"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurerest~Rest"));
    }

    @Test
    public void testAlternateRestRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/rest/this/is/the/rest").then()
                .body(containsString("Rest: /this/is/the/rest"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/rest~Rest"));
    }

    @Test
    public void testAlternateRestFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurerest/this/is/the/rest")
                .then()
                .body(containsString("FutureRest: /this/is/the/rest"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurerest~Rest"));
    }

    @Test
    public void testSecondaryAlternateRestRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/rest").then()
                .body(containsString("Rest: "));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/rest~Rest"));
    }

    @Test
    public void testSecondaryAlternateRestFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurerest").then()
                .body(containsString("FutureRest: "));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurerest~Rest"));
    }

    @Test
    public void testRestPathRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/restpath/rest").then()
                .body(containsString("RestPath: rest"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/restpath/RestPath"));
    }

    @Test
    public void testRestPathFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurerestpath/rest").then()
                .body(containsString("FutureRestPath: rest"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurerestpath/RestPath"));
    }

    @Test
    public void testIntNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/int/10").then()
                .body(containsString("IntNumber: 10"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/int/IntNumber"));
    }

    @Test
    public void testIntNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/intfuture/10").then()
                .body(containsString("IntNumberFuture: 10"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/intfuture/IntNumber"));
    }

    @Test
    public void testLongNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/long/1337").then()
                .body(containsString("LongNumber: 1337"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/long/LongNumber"));
    }

    @Test
    public void testLongNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/longfuture/1337").then()
                .body(containsString("LongNumberFuture: 1337"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/longfuture/LongNumber"));
    }

    @Test
    public void testHexIntNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/hexint/CAFE").then()
                .body(containsString("HexIntNumber: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexint/HexIntNumber"));
    }

    @Test
    public void testHexIntNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/hexintfuture/CAFE")
                .then()
                .body(containsString("HexIntNumberFuture: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexintfuture/HexIntNumber"));
    }

    @Test
    public void testHexLongNumberRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/hexlong/CAFE").then()
                .body(containsString("HexLongNumber: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexlong/HexLongNumber"));
    }

    @Test
    public void testHexLongNumberFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/hexlongfuture/CAFE")
                .then()
                .body(containsString("HexLongNumberFuture: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/hexlongfuture/HexLongNumber"));
    }

    @Test
    public void testDoubleNumberRoute() {
        String doubleRegex = "[+-]?\\d*\\.?\\d*";
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/double/123.456").then()
                .body(containsString("DoubleNumber: 123.456"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/double/" + doubleRegex));
    }

    @Test
    public void testDoubleNumberFutureRoute() {
        String doubleRegex = "[+-]?\\d*\\.?\\d*";
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/double/future/123.456")
                .then()
                .body(containsString("DoubleNumberFuture: 123.456"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/double/future/" + doubleRegex));
    }

    @Test
    public void testSegmentsRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/segments/here/are/segments")
                .then()
                .body(containsString("Segments: here,are,segments"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/segments/(Segment/).repeat()"));
    }

    @Test
    public void testSegmentsFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futuresegments/here/are/segments")
                .then().body(containsString("FutureSegments: here,are,segments"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futuresegments/(Segment/).repeat()"));
    }

    @Test
    public void testComplexRepeatRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/repeat/52/53/complex").then()
                .body(containsString("Repeat: 52,53"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/repeat/(IntNumber/).repeat()/complex"));
    }

    @Test
    public void testComplexRepeatFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurerepeat/52/53/complex").then()
                .body(containsString("FutureRepeat: 52,53"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futurerepeat/(IntNumber/).repeat()/complex"));
    }

    @Test
    public void testAlternativeComplexRepeatRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/repeat/52/53/54/complex").then()
                .body(containsString("Repeat: 52,53,54"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/repeat/(IntNumber/).repeat()/complex"));
    }

    @Test
    public void testAlternativeComplexRepeatFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurerepeat/52/53/54/complex").then()
                .body(containsString("FutureRepeat: 52,53,54"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futurerepeat/(IntNumber/).repeat()/complex"));
    }

    @Test
    public void testPipeRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pipe/i5").then()
                .body(containsString("Pipe: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/pipe/i~IntNumber"));
    }

    @Test
    public void testFuturePipeRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurepipe/i5").then()
                .body(containsString("FuturePipe: 5"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurepipe/i~IntNumber"));
    }

    @Test
    public void testAlternativePipeRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pipe/hCAFE").then()
                .body(containsString("Pipe: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/pipe/h~HexIntNumber"));
    }

    @Test
    public void testAlternativePipeFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurepipe/hCAFE").then()
                .body(containsString("FuturePipe: 51966"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(
                introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurepipe/h~HexIntNumber"));
    }

    @Test
    public void testPipeOptionalRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pipe/optional/X/create").then()
                .body(containsString("Pipe + Optional: null"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/pipe/optional/X~IntNumber.?/create"));
    }

    @Test
    public void testPipeOptionalFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurepipe/optional/X/create")
                .then()
                .body(containsString("FuturePipe + Optional: null"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futurepipe/optional/X~IntNumber.?/create"));
    }

    @Test
    public void testAlternativePipeOptionalRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/pipe/optional/X71/edit").then()
                .body(containsString("Pipe + Optional: 71"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/pipe/optional/X~IntNumber.?/edit"));
    }

    @Test
    public void testAlternativePipeOptionalFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurepipe/optional/X71/edit")
                .then()
                .body(containsString("FuturePipe + Optional: 71"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                getTransactionPrefix() + "/futurepipe/optional/X~IntNumber.?/edit"));
    }

    @Test
    public void testNegationRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/match").then()
                .body(containsString("Negation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/match~!nomatch"));
    }

    @Test
    public void testNegationFutureRoute() throws InterruptedException {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurematch").then()
                .body(containsString("FutureNegation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Thread.sleep(5000);
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurematch~!nomatch"));
    }

    @Test
    public void testAlternativeNegationRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/matchno").then()
                .body(containsString("Negation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/match~!nomatch"));
    }

    @Test
    public void testAlternativeNegationFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurematchno").then()
                .body(containsString("FutureNegation"));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/futurematch~!nomatch"));
    }

    @Test
    public void testUnmatchedNegationRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/matchnomatch")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testUnmatchedNegationFutureRoute() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/futurematchnomatch")
                .then().body(containsString("The requested resource could not be found."));

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
		Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(getTransactionPrefix() + "/Unknown Route"));
    }

    @Test
    public void testNoMGI() {
        given()
                .baseUri("http://localhost:" + server.getPort())
                .when()
                .get("/prefix-first")
                .then()
                .body(containsString("prefix-first"));

        String expectedTransactionName = getTransactionPrefix() + "/prefix-first";

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_MILLIS));
        Assert.assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains(
                expectedTransactionName));

        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        Assert.assertTrue(metricsForTransaction.containsKey("Akka/temp/tell/akka.io.TcpIncomingConnection"));
        Assert.assertTrue(metricsForTransaction.containsKey("Akka/temp/tell/spray.can.server.HttpServerConnection"));

        Pattern pattern = Pattern.compile("Akka/\\d+/tell");
        Matcher matcher = pattern.matcher("");
        for (String metricName : metricsForTransaction.keySet()) {
            matcher.reset(metricName);
            Assert.assertFalse(matcher.find());
        }
    }


    private String getTransactionPrefix() {
        return "WebTransaction/SprayHttp";
    }
}
