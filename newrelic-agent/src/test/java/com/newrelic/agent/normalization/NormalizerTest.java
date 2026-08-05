/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class NormalizerTest {

    private static final String APP_NAME = "Unit Test";

    @Test
    public void normalize_withInvalidReplacementString_doesNotThrow() {
        // If the replacement string is a correct format but a group index is out of bounds
        // This rule is from the actual support case
        String match =
                "^MessageBroker/Kafka/Internal/(consumer-metrics|producer-metrics)/client/[^/]+/(.+)$";
        String replace = "MessageBroker/Kafka/Internal/\\1/client/Kafka-Producers/\\3";  // No group 3 in match String
        NormalizationRule rule = new NormalizationRule(match, replace, false, 1, true, false, false);
        List<NormalizationRule> rules = Collections.singletonList(rule);

        Normalizer normalizer = new NormalizerImpl("foo", rules);
        Assert.assertTrue(normalizer.getRules().contains(rule));

        // This should not throw an exception and the target String should be returned unmodified
        String result = normalizer.normalize(
                "MessageBroker/Kafka/Internal/producer-metrics/client/my-producer-id/record-send-rate");
        Assert.assertEquals(
                "MessageBroker/Kafka/Internal/producer-metrics/client/my-producer-id/record-send-rate", result);

        // ... and the list should now be empty since the bad rule gets removed
        Assert.assertTrue(normalizer.getRules().isEmpty());
    }

    @Test
    public void normalize_withBadRuleInMiddleOfChain_appliesRemainingRulesAndRemovesOnlyBadRule() {
        NormalizationRule goodRule1 = new NormalizationRule("^foo/(.*)$", "baz/\\1", false, 1, false, false, false);
        // Matches "baz/bar/baz" with 2 groups but the replacement references a non-existent group 3
        NormalizationRule badRule = new NormalizationRule("^baz/(.*)/(.*)$", "baz/\\1/\\2/\\3", false, 2, false, false,
                false);
        NormalizationRule goodRule2 = new NormalizationRule("^baz/(.*)$", "final/\\1", false, 3, true, false, false);
        List<NormalizationRule> rules = Arrays.asList(goodRule1, badRule, goodRule2);

        Normalizer normalizer = new NormalizerImpl("foo", rules);

        // both good rules should apply, in order, despite the bad rule throwing in between them
        Assert.assertEquals("final/bar/baz", normalizer.normalize("foo/bar/baz"));

        // only the bad rule should have been removed, in place, leaving the good rules intact
        Assert.assertEquals(Arrays.asList(goodRule1, goodRule2), normalizer.getRules());

        // a second call must not re-throw and should produce the same result using only the surviving rules
        Assert.assertEquals("final/bar/baz", normalizer.normalize("foo/bar/baz"));
        Assert.assertEquals(2, normalizer.getRules().size());
    }

    @Test
    public void normalize_withBadRuleMarkedTerminateChain_stillAppliesSubsequentRules() {
        NormalizationRule goodRule1 = new NormalizationRule("^foo/(.*)$", "bar/\\1", false, 1, false, false, false);
        // terminateChain is true here, but that flag must not take effect because the rule throws instead of matching
        NormalizationRule badRule = new NormalizationRule("^bar/(.*)/(.*)$", "bar/\\1/\\2/\\3", false, 2, true,
                false, false);
        NormalizationRule goodRule2 = new NormalizationRule("^bar/(.*)$", "baz/\\1", false, 3, false, false, false);
        List<NormalizationRule> rules = Arrays.asList(goodRule1, badRule, goodRule2);

        Normalizer normalizer = new NormalizerImpl("foo", rules);

        // goodRule2 must still run even though the removed rule ahead of it was marked terminateChain=true
        Assert.assertEquals("baz/boo/fizz", normalizer.normalize("foo/boo/fizz"));
        Assert.assertEquals(Arrays.asList(goodRule1, goodRule2), normalizer.getRules());

        // a second call must not re-throw and should produce the same result using only the surviving rules
        Assert.assertEquals("baz/boo/fizz", normalizer.normalize("foo/boo/fizz"));
        Assert.assertEquals(2, normalizer.getRules().size());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void emptyUrlRules() {
        final JSONArray rulesData = new JSONArray();
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer(APP_NAME, rules);
        Assert.assertEquals(0, normalizer.getRules().size());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void urlRulesSegmentDelimiter() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/LAST_NAME");
                put("replacement", "/fail");
                put("eval_order", 1);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Normalizer normalizer = NormalizerFactory.createUrlNormalizer(APP_NAME, rules);
        Assert.assertEquals(1, normalizer.getRules().size());
        Assert.assertEquals("/fail", normalizer.normalize("/LAST_NAME"));
        Assert.assertEquals("/fail", normalizer.normalize("LAST_NAME"));

        // make sure null will work
        Assert.assertNull(normalizer.normalize(null));
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void transactionNormalizer() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^(Apdex|WebTransaction)/(.*)/betting/.*$");
                put("replacement", "\\1/\\2/betting/*");
                put("eval_order", 1);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getTransactionNameRules(APP_NAME, rulesData);
        Normalizer normalizer = NormalizerFactory.createTransactionNormalizer(APP_NAME, rules,
                Collections.<TransactionSegmentTerms> emptyList());
        Assert.assertEquals(1, normalizer.getRules().size());
        Assert.assertEquals("WebTransaction/ru/betting/*", normalizer.normalize("WebTransaction/ru/betting/Motorsport"));
        String txName = "OtherTransaction/ru/betting/Motorsport";
        Assert.assertSame(txName, normalizer.normalize(txName));
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void metricNormalizer() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^CUSTOM/(.*)/betting/.*$");
                put("replacement", "CUSTOM/\\1/betting/*");
                put("eval_order", 1);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getMetricNameRules(APP_NAME, rulesData);
        Normalizer normalizer = NormalizerFactory.createMetricNormalizer(APP_NAME, rules);
        Assert.assertEquals(1, normalizer.getRules().size());
        Assert.assertEquals("CUSTOM/ru/betting/*", normalizer.normalize("CUSTOM/ru/betting/Motorsport"));
        String txName = "NOT_CUSTOM/ru/betting/Motorsport";
        Assert.assertSame(txName, normalizer.normalize(txName));

        // make sure null will work
        Assert.assertNull(normalizer.normalize(null));
    }

    @SuppressWarnings({ "unchecked", "serial" })
    // @Test
    public void performance() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^artists/az/(.*)/(.*)$");
                put("replacement", "artists/az/*/\\1");
                put("eval_order", 10);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^ei/app/modules/customer/[0-9]+/(.*)");
                put("replacement", "^ei/app/modules/customer/*/\\1");
                put("eval_order", 20);
            }
        }, new JSONObject() {
            {
                put("match_expression", "^([^/]*)/betting/([A-Z]*)([^/]*)/.*$");
                put("replacement", "\\1/betting/\\2\\3/*");
                put("eval_order", 30);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getMetricNameRules(APP_NAME, rulesData);
        Normalizer normalizer = NormalizerFactory.createMetricNormalizer(APP_NAME, rules);
        Assert.assertEquals("en/betting/Football/*", normalizer.normalize("en/betting/Football/USA/MLS"));

        String metric = "en/betting/Football/USA/MLS";
        long startTime = System.currentTimeMillis();
        int count = 1000000;
        for (int i = 0; i < count; i++) {
            normalizer.normalize(metric);
        }
        long stopTime = System.currentTimeMillis();
        String msg = MessageFormat.format("{0} iterations took {1} milliseconds ({2} nanoseconds per iteration)",
                count, stopTime - startTime, (stopTime - startTime) * 1000 / count);
        System.out.println(msg);
    }

}
