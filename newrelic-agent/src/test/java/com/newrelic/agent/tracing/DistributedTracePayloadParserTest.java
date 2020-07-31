/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import java.util.Map;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class DistributedTracePayloadParserTest extends BaseDistributedTraceTest {

    @Test
    public void testParseInvalidPayload() {
        DistributedTracePayloadParser parser = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(), ServiceFactory.getDistributedTraceService(), noOpLogger);
        assertNull(parser.parse(null, ""));

        String incompletePayload = "{\"v\" : [0,2]}";
        assertNull(parser.parse(null, incompletePayload));
    }

    @Test
    public void testIncorrectPayloadTrustKey() {
        createDistributedTraceService("12345", "67890", "67890", 0, 2);

        String payloadWithIncorrectTrustKey =
                "{" +
                        "  \"v\": [0,1]," +
                        "  \"d\": {" +
                        "    \"ty\": \"App\"," +
                        "    \"ac\": \"12345\"," +
                        "    \"tk\": \"09876\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"tx\": \"abcdefghijk\"," +
                        "    \"pr\": 0.5," +
                        "    \"ti\": 1482959525577," +
                        "  }" +
                        "}";

        DistributedTracePayloadParser parser = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(),  ServiceFactory.getDistributedTraceService(),
            noOpLogger);
        DistributedTracePayloadImpl payload = parser.parse(null, payloadWithIncorrectTrustKey);
        assertNull(payload);
    }

    @Test
    public void testPayloadParsing() {
        createDistributedTraceService("accountId", "67890", "67890", 0, 2);

        String payloadWithIncorrectAccountId =
                "{" +
                        "  \"v\": [0,1]," +
                        "  \"d\": {" +
                        "    \"ty\": \"App\"," +
                        "    \"ac\": \"12345\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"," +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"tx\": \"abcdefghijk\"," +
                        "    \"pr\": 0.0," +
                        "    \"ti\": 1482959525577," +
                        "  }" +
                        "}";

        DistributedTracePayloadParser parse = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(),  ServiceFactory.getDistributedTraceService(),
            noOpLogger);
        DistributedTracePayloadImpl payload = parse.parse(null, payloadWithIncorrectAccountId);
        assertNotNull(payload);
        assertEquals("App", payload.parentType);
        assertEquals("12345", payload.accountId);
        assertEquals("51424", payload.applicationId);
        assertEquals("27856f70d3d314b7", payload.guid);
        assertEquals("67890", payload.trustKey);
        assertEquals("3221bf09aa0bcf0d", payload.traceId);
        assertTrue(payload.priority < 1.0f);
    }

    @Test
    public void testPayloadParseCallerTxn() {
        createDistributedTraceService("12345", "67890", "67890", 0, 2);

        String payloadString =
                "{" +
                        "  \"v\": [0,1]," +
                        "  \"d\": {" +
                        "    \"ty\": \"App\"," +
                        "    \"ac\": \"12345\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"," +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"tx\": \"abcdefghijk\"," +
                        "    \"pr\": 0.0," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";

        DistributedTracePayloadParser parser = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(),  ServiceFactory.getDistributedTraceService(),
            noOpLogger);
        DistributedTracePayloadImpl payload = parser.parse(null, payloadString);
        assertNotNull(payload);
        assertEquals("abcdefghijk", payload.txnId);

        assertEquals("App", payload.parentType);
        assertEquals("12345", payload.accountId);
        assertEquals("51424", payload.applicationId);
        assertEquals("27856f70d3d314b7", payload.guid);
        assertEquals("3221bf09aa0bcf0d", payload.traceId);
        assertTrue(payload.priority < 1.0f);
    }


    @Test
    public void testTrustKeyOptimizationOutboundPayload() throws ParseException {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);

        Transaction transaction = Transaction.getTransaction(true);
        DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload(transaction.getGuid(), transaction.getGuid(),
                transaction.getGuid(), 1.0f);

        JSONParser parser = new JSONParser();
        Map<String, Map<String, String>> parsedPayload = (Map<String, Map<String, String>>) parser.parse(payload.text());
        // Include tk when account_id != trust_key
        assertEquals("trustKey", parsedPayload.get("d").get("tk"));

        createDistributedTraceService("accountId", "accountId", "appID", 0, 2);
        payload = DistributedTracePayloadImpl.createDistributedTracePayload(transaction.getGuid(), transaction.getGuid(), transaction.getGuid(), 1.0f);
        parser = new JSONParser();
        parsedPayload = (Map<String, Map<String, String>>) parser.parse(payload.text());
        // Don't include tk since account_id == trust_key
        assertNull(parsedPayload.get("d").get("tk"));
    }

    @Test
    public void testTrustKeyOptimizationInboundPayload() {
        createDistributedTraceService("accountId", "accountId", "appID", 0, 2);

        String payloadString =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"App\"," +
                        "    \"ac\": \"accountId\"," +
                        "    \"ap\": \"8747\"," +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"tx\": \"abcdefghijk\"," +
                        "    \"pr\": 0.5," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";

        DistributedTracePayloadParser parser = new DistributedTracePayloadParser(serviceManager.getStatsService().getMetricAggregator(),  serviceManager.getDistributedTraceService(),
            noOpLogger);
        DistributedTracePayloadImpl tkMissingPayload = parser.parse(null, payloadString);
        assertNotNull(tkMissingPayload);
        assertEquals("accountId", tkMissingPayload.accountId);
        assertEquals("8747", tkMissingPayload.applicationId);
        assertNull(tkMissingPayload.trustKey);
        assertEquals("27856f70d3d314b7", tkMissingPayload.guid);
        assertEquals("3221bf09aa0bcf0d", tkMissingPayload.traceId);
        assertEquals("abcdefghijk", tkMissingPayload.txnId);
        assertEquals(0.5f, tkMissingPayload.priority, 0.0001);
        assertEquals(1482959525577L, tkMissingPayload.timestamp);

        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);
        parser = new DistributedTracePayloadParser(serviceManager.getStatsService().getMetricAggregator(),  serviceManager.getDistributedTraceService(),
            noOpLogger);
        payloadString =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"App\"," +
                        "    \"ac\": \"12345\"," +
                        "    \"ap\": \"51424\"," +
                        "    \"tk\": \"trustKey\"," +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"tx\": \"abcdefghijk\"," +
                        "    \"pr\": 0.7," +
                        "    \"ti\": 1482959535577" +
                        "  }" +
                        "}";

        DistributedTracePayloadImpl payloadWithTk = parser.parse(null, payloadString);
        assertEquals("12345", payloadWithTk.accountId);
        assertEquals("51424", payloadWithTk.applicationId);
        assertEquals("trustKey", payloadWithTk.trustKey);
        assertEquals("27856f70d3d314b7", payloadWithTk.guid);
        assertEquals("3221bf09aa0bcf0d", payloadWithTk.traceId);
        assertEquals("abcdefghijk", payloadWithTk.txnId);
        assertEquals(0.7f, payloadWithTk.priority, 0.0001);
        assertEquals(1482959535577L, payloadWithTk.timestamp);
    }

    @Test
    public void testNullTrustKeyReturnedByDistributedTraceServices() {
        createDistributedTraceService("accountId", null, "67890", 0, 2);

        String payloadString =
                "{" +
                        "  \"v\": [0,1]," +
                        "  \"d\": {" +
                        "    \"ty\": \"App\"," +
                        "    \"ac\": \"12345\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"," +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"tx\": \"abcdefghijk\"," +
                        "    \"pr\": 0.0," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";

        DistributedTracePayloadParser parser = new DistributedTracePayloadParser(
            NewRelic.getAgent().getMetricAggregator(),  ServiceFactory.getDistributedTraceService(), noOpLogger);
        DistributedTracePayloadImpl payload = parser.parse(null, payloadString);
        assertNull(payload);
    }
}