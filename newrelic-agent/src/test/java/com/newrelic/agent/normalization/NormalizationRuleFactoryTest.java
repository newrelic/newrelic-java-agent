/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class NormalizationRuleFactoryTest {

    private static final String APP_NAME = "Unit Test";

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void emptyRules() {
        final JSONArray rulesData = new JSONArray();
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Assert.assertEquals(0, rules.size());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void oneRule() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/LAST_NAME");
                put("replacement", "/fail");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.FALSE);
                put("replace_all", Boolean.FALSE);
                put("terminate_chain", Boolean.TRUE);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Assert.assertEquals(1, rules.size());
        NormalizationRule rule = rules.get(0);
        Assert.assertEquals("/LAST_NAME", rule.getMatchExpression());
        Assert.assertEquals("/fail", rule.getReplacement());
        Assert.assertFalse(rule.isIgnore());
        Assert.assertEquals(1, rule.getOrder());
        Assert.assertFalse(rule.isEachSegment());
        Assert.assertFalse(rule.isReplaceAll());
        Assert.assertTrue(rule.isTerminateChain());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void multipleRules() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^[0-9a-f]*[0-9][0-9a-f]*$");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 10);
                put("each_segment", Boolean.TRUE);
                put("replace_all", Boolean.FALSE);
                put("terminate_chain", Boolean.FALSE);
            }
        }, new JSONObject() {
            {
                put("match_expression", "(.*)/[^/]*.(bmp|css|gif|ico|jpg|jpeg|js|png)$");
                put("replacement", "\\1/*.\\2");
                put("ignore", Boolean.FALSE);
                put("eval_order", 20);
                put("each_segment", Boolean.FALSE);
                put("replace_all", Boolean.FALSE);
                put("terminate_chain", Boolean.TRUE);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Assert.assertEquals(2, rules.size());
        NormalizationRule rule = rules.get(0);
        Assert.assertEquals("^[0-9a-f]*[0-9][0-9a-f]*$", rule.getMatchExpression());
        Assert.assertEquals("*", rule.getReplacement());
        Assert.assertFalse(rule.isIgnore());
        Assert.assertEquals(10, rule.getOrder());
        Assert.assertTrue(rule.isEachSegment());
        Assert.assertFalse(rule.isReplaceAll());
        Assert.assertFalse(rule.isTerminateChain());

        rule = rules.get(1);
        Assert.assertEquals("(.*)/[^/]*.(bmp|css|gif|ico|jpg|jpeg|js|png)$", rule.getMatchExpression());
        Assert.assertEquals("$1/*.$2", rule.getReplacement());
        Assert.assertFalse(rule.isIgnore());
        Assert.assertEquals(20, rule.getOrder());
        Assert.assertFalse(rule.isEachSegment());
        Assert.assertFalse(rule.isReplaceAll());
        Assert.assertTrue(rule.isTerminateChain());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void replacementBackslashes() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "(.*)/[^/]*.(bmp|css|gif|ico|jpg|jpeg|js|png)$");
                put("replacement", "\\1/*.\\2");
                put("eval_order", 1);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Assert.assertEquals(1, rules.size());
        NormalizationRule rule = rules.get(0);
        Assert.assertEquals("$1/*.$2", rule.getReplacement());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void defaults() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/LAST_NAME");
                put("replacement", "/fail");
                put("eval_order", 1);
            }
        }));
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Assert.assertEquals(1, rules.size());
        NormalizationRule rule = rules.get(0);
        Assert.assertEquals("/LAST_NAME", rule.getMatchExpression());
        Assert.assertEquals("/fail", rule.getReplacement());
        Assert.assertFalse(rule.isIgnore());
        Assert.assertEquals(1, rule.getOrder());
        Assert.assertFalse(rule.isEachSegment());
        Assert.assertFalse(rule.isReplaceAll());
        Assert.assertTrue(rule.isTerminateChain());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void metricNameRules() {
        final JSONArray rulesData = new JSONArray();
        rulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "/LAST_NAME");
                put("replacement", "/fail");
                put("ignore", Boolean.FALSE);
                put("eval_order", 1);
                put("each_segment", Boolean.FALSE);
                put("replace_all", Boolean.FALSE);
                put("terminate_chain", Boolean.TRUE);
            }
        }));

        final JSONArray metricRulesData = new JSONArray();
        metricRulesData.addAll(Arrays.asList(new JSONObject() {
            {
                put("match_expression", "^[0-9a-f]*[0-9][0-9a-f]*$");
                put("replacement", "*");
                put("ignore", Boolean.FALSE);
                put("eval_order", 10);
                put("each_segment", Boolean.TRUE);
                put("replace_all", Boolean.FALSE);
                put("terminate_chain", Boolean.FALSE);
            }
        }));

        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules(APP_NAME, rulesData);
        Assert.assertEquals(1, rules.size());
        NormalizationRule rule = rules.get(0);
        Assert.assertEquals("/LAST_NAME", rule.getMatchExpression());
        Assert.assertEquals("/fail", rule.getReplacement());
        Assert.assertFalse(rule.isIgnore());
        Assert.assertEquals(1, rule.getOrder());
        Assert.assertFalse(rule.isEachSegment());
        Assert.assertFalse(rule.isReplaceAll());
        Assert.assertTrue(rule.isTerminateChain());

        rules = NormalizationRuleFactory.getMetricNameRules(APP_NAME, metricRulesData);
        Assert.assertEquals(1, rules.size());
        rule = rules.get(0);
        Assert.assertEquals("^[0-9a-f]*[0-9][0-9a-f]*$", rule.getMatchExpression());
        Assert.assertEquals("*", rule.getReplacement());
        Assert.assertFalse(rule.isIgnore());
        Assert.assertEquals(10, rule.getOrder());
        Assert.assertTrue(rule.isEachSegment());
        Assert.assertFalse(rule.isReplaceAll());
        Assert.assertFalse(rule.isTerminateChain());
    }

}
