/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class NormalizationRuleConfig {
    public static final String URL_RULES_KEY = "url_rules";
    public static final String TRANSACTION_SEGMENT_TERMS_KEY = "transaction_segment_terms";
    public static final String METRIC_NAME_RULES_KEY = "metric_name_rules";
    public static final String TRANSACTION_NAME_RULES_KEY = "transaction_name_rules";
    private static final Logger logger = Agent.LOG.getChildLogger(NormalizationRuleConfig.class);

    private final List<Map<String, Object>> urlRules;
    private final List<Map<String, Object>> metricNameRules;
    private final List<Map<String, Object>> transactionNameRules;
    private final List<Map<String, Object>> transactionSegmentRules;

    public NormalizationRuleConfig(Map<String, Object> data) {
        urlRules = initListFromData(data, URL_RULES_KEY);
        metricNameRules = initListFromData(data, METRIC_NAME_RULES_KEY);
        transactionNameRules = initListFromData(data, TRANSACTION_NAME_RULES_KEY);
        transactionSegmentRules = initListFromData(data, TRANSACTION_SEGMENT_TERMS_KEY);
    }

    public List<Map<String, Object>> getMetricNameRules() {
        return metricNameRules;
    }

    public List<Map<String, Object>> getTransactionNameRules() {
        return transactionNameRules;
    }

    public List<Map<String, Object>> getTransactionSegmentRules() {
        return transactionSegmentRules;
    }

    public List<Map<String, Object>> getUrlRules() {
        return urlRules;
    }

    public List<Map<String, Object>> initListFromData(Map<String, Object> data, String key) {
        List<Map<String, Object>> result = Collections.emptyList();
        Object value = data.get(key);
        if (value instanceof ServerProp) {
            value = ((ServerProp)value).getValue();
        }

        if (value instanceof List) {
            try {
                result = (List<Map<String, Object>>) value;
            } catch (ClassCastException exc) {
                logger.log(Level.FINE, "The server provided an object of type {0} for {1}, expected a List", value.getClass(), key);
            }
        }

        return result;
    }
}
