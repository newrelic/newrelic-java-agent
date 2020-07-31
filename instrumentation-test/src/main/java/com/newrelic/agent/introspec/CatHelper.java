/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.service.ServiceUtils;
import org.junit.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatHelper {

    public static final String CAT_EXTERNAL_TX_NAME = "WebTransaction/Custom/ExternalHTTPServer";
    // ExternalTransaction/{host}/{cross_process_id}/{transaction_name}
    public static final Pattern EXTERAL_TX_SCOPED_METRIC = Pattern.compile("ExternalTransaction/([^/]+)/([^/]+)/(.+)");

    public static void verifyOneSuccessfulCat(Introspector intro, String txName) {
        verifyOneSuccessfulCat(intro, txName, CAT_EXTERNAL_TX_NAME);
    }

    /**
     * @param introspector
     * @param txName name of transaction that originated CAT request
     * @param externalTransaction name of transaction that responded to CAT request
     */
    public static void verifyOneSuccessfulCat(Introspector introspector, String txName, String externalTransaction) {
        verifyMultipleSuccessfulCat(introspector, txName, externalTransaction, 1);
    }

    public static void verifyMultipleSuccessfulCat(Introspector introspector, String txName, int count) {
        verifyMultipleSuccessfulCat(introspector, txName, CAT_EXTERNAL_TX_NAME, count);
    }

    public static void verifyMultipleSuccessfulCat(Introspector introspector, String txName, String externalTransaction, int count) {
        Collection<String> txNames = introspector.getTransactionNames();

        // check tx names
        Assert.assertTrue(txNames.size() >= 2);
        Assert.assertTrue("externalTransaction metric missing: " + externalTransaction, txNames.contains(externalTransaction));
        Assert.assertTrue(txNames.contains(txName));

        String crossProcessId = verifyMetricsAndReturnCrossProcessId(introspector, txName, externalTransaction, count);
        verifyTransactionEventsAndTraces(introspector, txName, externalTransaction, crossProcessId, count);
    }

    private static void verifyTransactionEventsAndTraces(Introspector intro, String txName, String externalTxnName, String crossProcessId, int count) {
        // transaction events
        Collection<TransactionEvent> sentEvents = intro.getTransactionEvents(txName);
        Assert.assertEquals(count, sentEvents.size());
        TransactionEvent sent = sentEvents.iterator().next();

        Collection<TransactionEvent> receiveEvents = intro.getTransactionEvents(externalTxnName);
        Assert.assertEquals(count, receiveEvents.size());
        TransactionEvent receive = receiveEvents.iterator().next();

        // The trip id should be consistent across the entire trip
        Assert.assertNotNull(sent.getTripId());
        Assert.assertEquals(sent.getTripId(), receive.getTripId());

        Assert.assertEquals(sent.getMyGuid(), receive.getReferrerGuid());

        /*
         * The path hash can change during the transaction. If it changes, then the initial path hash will not equal the
         * referring path has.
         */
        String alternate = sent.getMyAlternatePathHashes();
        if (alternate == null) {
            Assert.assertEquals(sent.getMyPathHash().intValue(), receive.getReferringPathHash().intValue());
        } else {
            Assert.assertNotEquals(sent.getMyPathHash().intValue(), receive.getReferringPathHash().intValue());
        }

        // also check the transaction trace
        Collection<TransactionTrace> sendTraces = intro.getTransactionTracesForTransaction(txName);
        Assert.assertEquals(count, sendTraces.size());
        TransactionTrace sendTrace = sendTraces.iterator().next();
        Map<String, Object> intrinsicsSend = sendTrace.getIntrinsicAttributes();

        Collection<TransactionTrace> receiveTraces = intro.getTransactionTracesForTransaction(externalTxnName);
        Assert.assertEquals(count, receiveTraces.size());
        TransactionTrace receiveTrace = receiveTraces.iterator().next();
        Map<String, Object> intrinsicsReceive = receiveTrace.getIntrinsicAttributes();

        Assert.assertEquals(crossProcessId,
                (String) intrinsicsReceive.get(AttributeNames.CLIENT_CROSS_PROCESS_ID_PARAMETER_NAME));
        // trip ids should all be the same
        Assert.assertEquals(sent.getTripId(), (String) intrinsicsSend.get("trip_id"));
        Assert.assertEquals(sent.getTripId(), (String) intrinsicsReceive.get("trip_id"));

        // path hash on send and referrer guid on receive
        Assert.assertEquals(ServiceUtils.intToHexString(sent.getMyPathHash()), intrinsicsSend.get("path_hash"));
        Assert.assertEquals(ServiceUtils.intToHexString(receive.getMyPathHash()), intrinsicsReceive.get("path_hash"));

        Assert.assertEquals(sent.getMyGuid(), (String) intrinsicsReceive.get("referring_transaction_guid"));
    }

    /*
     * Metric iff a valid trusted account id was extracted - external side: ClientApplication/{cross_process_id}/all
     *
     * Metrics iff app data received - sending side: ExternalApp/{host}/{cross_process_id}/all,
     * ExternalTransaction/{host}/{cross_process_id}/{transaction_name}
     *
     * scoped iff app data received - sending side: ExternalTransaction/{host}/{cross_process_id}/{transaction_name}
     */
    private static String verifyMetricsAndReturnCrossProcessId(Introspector intro, String txName, String externalTxName, int count) {
        Collection<ExternalRequest> externals = intro.getExternalRequests(txName);
        Assert.assertEquals(1, externals.size());

        ExternalRequest current = externals.iterator().next();
        Assert.assertNotNull(current.getTransactionGuild());
        Assert.assertEquals(count, current.getCount());

        // then it is a cat call
        String metricName = current.getMetricName();
        Matcher metricMatcher = EXTERAL_TX_SCOPED_METRIC.matcher(metricName);
        Assert.assertTrue(metricMatcher.matches());
        Assert.assertTrue(metricMatcher.groupCount() == 3);
        String host = metricMatcher.group(1);
        String crossProcessId = metricMatcher.group(2);
        String transactionNameFromReceive = metricMatcher.group(3);

        Assert.assertEquals(host, current.getHostname());

        // check that scoped metrics exists
        Map<String, TracedMetricData> sendingMetrics = intro.getMetricsForTransaction(txName);
        TracedMetricData data = sendingMetrics.get(metricName);
        Assert.assertNotNull(data);
        Assert.assertTrue(data.getCallCount() >= current.getCount());

        // check transaction exists on the receiving side
        Assert.assertEquals(externalTxName, transactionNameFromReceive);

        // ExternalTransaction/{host}/{cross_process_id}/{transaction_name} check
        Map<String, TracedMetricData> allUnscoped = intro.getUnscopedMetrics();
        data = allUnscoped.get(metricName);
        Assert.assertNotNull(data);
        Assert.assertTrue(data.getCallCount() >= current.getCount());

        // ExternalApp/{host}/{cross_process_id}/all check
        // We don't create this metric when host is Unknown
        if (!"Unknown".equals(host)) {
            data = allUnscoped.get("ExternalApp/" + host + "/" + crossProcessId + "/all");
            Assert.assertNotNull(data);
            Assert.assertTrue(data.getCallCount() >= current.getCount());
        }

        // ClientApplication/{cross_process_id}/all
        data = allUnscoped.get("ClientApplication/" + crossProcessId + "/all");
        Assert.assertNotNull(data);
        Assert.assertTrue(data.getCallCount() >= current.getCount());

        return crossProcessId;
    }
}
