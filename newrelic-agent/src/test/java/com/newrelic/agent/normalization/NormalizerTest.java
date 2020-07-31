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
