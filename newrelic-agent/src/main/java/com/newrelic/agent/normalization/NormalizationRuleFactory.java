/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is responsible for transforming the rules from the serialized format to typed.
 */
public class NormalizationRuleFactory {

    public static List<NormalizationRule> getUrlRules(String appName, List<Map<String, Object>> rulesData) {
        try {
            List<NormalizationRule> rules = createRules(appName, rulesData);
            if (rules.isEmpty()) {
                Agent.LOG.warning("The agent did not receive any url rules from the New Relic server.");
            } else {
                String msg = MessageFormat.format("Received {0} url rule(s) for {1}", rules.size(), appName);
                Agent.LOG.fine(msg);
            }
            return rules;
        } catch (Exception e) {
            String msg = MessageFormat.format(
                    "An error occurred getting url rules for {0} from the New Relic server. This can indicate a problem with the agent rules supplied by the New Relic server.: {1}",
                    appName, e);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }
        }
        return Collections.emptyList();
    }

    public static List<NormalizationRule> getMetricNameRules(String appName, List<Map<String, Object>> rulesData) {
        try {
            List<NormalizationRule> rules = createRules(appName, rulesData);
            String msg = MessageFormat.format("Received {0} metric name rule(s) for {1}", rules.size(), appName);
            Agent.LOG.fine(msg);
            return rules;
        } catch (Exception e) {
            String msg = MessageFormat.format(
                    "An error occurred getting metric name rules for {0} from the New Relic server. This can indicate a problem with the agent rules supplied by the New Relic server.: {1}",
                    appName, e);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }
        }
        return Collections.emptyList();
    }

    public static List<NormalizationRule> getTransactionNameRules(String appName, List<Map<String, Object>> rulesData) {
        try {
            List<NormalizationRule> rules = createRules(appName, rulesData);
            String msg = MessageFormat.format("Received {0} transaction name rule(s) for {1}", rules.size(), appName);
            Agent.LOG.fine(msg);
            return rules;
        } catch (Exception e) {
            String msg = MessageFormat.format(
                    "An error occurred getting transaction name rules for {0} from the New Relic server. This can indicate a problem with the agent rules supplied by the New Relic server.: {1}",
                    appName, e);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.log(Level.INFO, msg);
            }
        }
        return Collections.emptyList();
    }

    public static List<TransactionSegmentTerms> getTransactionSegmentTermRules(String appName, List<Map<String, Object>> rulesData) {
        List<TransactionSegmentTerms> list = new ArrayList<>();
        for (Map<String, Object> map : rulesData) {
            List<String> terms = (List<String>) map.get("terms");
            String prefix = (String) map.get("prefix");

            TransactionSegmentTerms tst = new TransactionSegmentTerms(prefix, ImmutableSet.copyOf(terms));
            list.add(tst);
        }
        Agent.LOG.log(Level.FINE, "Received {0} transaction segment rule(s) for {1}", list.size(), appName);
        return list;
    }

    private static List<NormalizationRule> createRules(String appName, List<Map<String, Object>> rulesData)
            throws Exception {
        List<NormalizationRule> rules = new ArrayList<>();
        for (Map<String, Object> ruleData : rulesData) {
            NormalizationRule rule = createRule(ruleData);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Adding rule for \"{0}\": \"{1}\"", appName, rule);
                Agent.LOG.finer(msg);
            }
            rules.add(rule);
        }
        sortRules(rules);
        return rules;
    }

    private static void sortRules(List<NormalizationRule> rules) {
        rules.sort(Comparator.comparing(NormalizationRule::getOrder));
    }

    private static NormalizationRule createRule(Map<String, Object> ruleData) {
        Boolean eachSegment = (Boolean) ruleData.get("each_segment");
        if (eachSegment == null) {
            eachSegment = Boolean.FALSE;
        }
        Boolean replaceAll = (Boolean) ruleData.get("replace_all");
        if (replaceAll == null) {
            replaceAll = Boolean.FALSE;
        }
        Boolean ignore = (Boolean) ruleData.get("ignore");
        if (ignore == null) {
            ignore = Boolean.FALSE;
        }
        Boolean terminateChain = (Boolean) ruleData.get("terminate_chain");
        if (terminateChain == null) {
            terminateChain = Boolean.TRUE;
        }
        int order = ((Number) ruleData.get("eval_order")).intValue();
        String matchExpression = (String) ruleData.get("match_expression");
        String replacement = (String) ruleData.get("replacement");

        return new NormalizationRule(matchExpression, replacement, ignore, order, terminateChain, eachSegment,
                replaceAll);
    }

}
