/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NormalizationServiceImpl extends AbstractService implements NormalizationService, ConnectionListener {

    private static final Pattern PARAMETER_DELIMITER_PATTERN = Pattern.compile("(.*?)(\\?|#|;).*", Pattern.DOTALL);
    private static final List<NormalizationRule> EMPTY_RULES = Collections.emptyList();

    private final ConcurrentMap<String, Normalizer> urlNormalizers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Normalizer> transactionNormalizers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Normalizer> metricNormalizers = new ConcurrentHashMap<>();
    private volatile Normalizer defaultUrlNormalizer;
    private volatile Normalizer defaultTransactionNormalizer;
    private volatile Normalizer defaultMetricNormalizer;
    private final String defaultAppName;
    private final boolean autoAppNamingEnabled;

    public NormalizationServiceImpl() {
        super(NormalizationService.class.getSimpleName());
        AgentConfig defaultAgentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        defaultAppName = defaultAgentConfig.getApplicationName();
        autoAppNamingEnabled = defaultAgentConfig.isAutoAppNamingEnabled();
        defaultUrlNormalizer = createUrlNormalizer(defaultAppName, EMPTY_RULES);
        defaultTransactionNormalizer = createTransactionNormalizer(defaultAppName, EMPTY_RULES,
                Collections.<TransactionSegmentTerms> emptyList());
        defaultMetricNormalizer = createMetricNormalizer(defaultAppName, EMPTY_RULES);
        ServiceFactory.getRPMServiceManager().addConnectionListener(this);
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
    }

    @Override
    public String getUrlBeforeParameters(String url) {
        Matcher paramDelimiterMatcher = PARAMETER_DELIMITER_PATTERN.matcher(url);
        if (paramDelimiterMatcher.matches()) {
            return paramDelimiterMatcher.group(1);
        } else {
            return url;
        }
    }

    @Override
    public Normalizer getUrlNormalizer(String appName) {
        return getOrCreateUrlNormalizer(appName);
    }

    @Override
    public Normalizer getTransactionNormalizer(String appName) {
        return getOrCreateTransactionNormalizer(appName);
    }

    @Override
    public Normalizer getMetricNormalizer(String appName) {
        return getOrCreateMetricNormalizer(appName);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void connected(IRPMService rpmService, AgentConfig agentConfig) {
        String appName = rpmService.getApplicationName();
        List<NormalizationRule> urlRules = NormalizationRuleFactory.getUrlRules(
                appName,
                agentConfig.getNormalizationRuleConfig().getUrlRules()
        );
        List<NormalizationRule> metricNameRules = NormalizationRuleFactory.getMetricNameRules(
                appName,
                agentConfig.getNormalizationRuleConfig().getMetricNameRules()
        );
        List<NormalizationRule> transactionNameRules = NormalizationRuleFactory.getTransactionNameRules(
                appName,
                agentConfig.getNormalizationRuleConfig().getTransactionNameRules()
        );
        List<TransactionSegmentTerms> transactionSegmentTermRules = NormalizationRuleFactory.getTransactionSegmentTermRules(
                appName,
                agentConfig.getNormalizationRuleConfig().getTransactionSegmentRules()
        );

        Normalizer normalizer = createUrlNormalizer(appName, urlRules);
        replaceUrlNormalizer(appName, normalizer);

        normalizer = createTransactionNormalizer(appName, transactionNameRules, transactionSegmentTermRules);
        replaceTransactionNormalizer(appName, normalizer);

        normalizer = createMetricNormalizer(appName, metricNameRules);
        replaceMetricNormalizer(appName, normalizer);
    }

    @Override
    public void disconnected(IRPMService rpmService) {
        // do nothing
    }

    private Normalizer getOrCreateUrlNormalizer(String appName) {
        Normalizer normalizer = findUrlNormalizer(appName);
        if (normalizer != null) {
            return normalizer;
        }
        normalizer = createUrlNormalizer(appName, EMPTY_RULES);
        Normalizer oldNormalizer = urlNormalizers.putIfAbsent(appName, normalizer);
        return oldNormalizer == null ? normalizer : oldNormalizer;
    }

    private Normalizer findUrlNormalizer(String appName) {
        if (!autoAppNamingEnabled || appName == null || appName.equals(defaultAppName)) {
            return defaultUrlNormalizer;
        }
        return urlNormalizers.get(appName);
    }

    private void replaceUrlNormalizer(String appName, Normalizer normalizer) {
        Normalizer oldNormalizer = getUrlNormalizer(appName);
        if (oldNormalizer == defaultUrlNormalizer) {
            defaultUrlNormalizer = normalizer;
        } else {
            urlNormalizers.put(appName, normalizer);
        }
    }

    private Normalizer getOrCreateTransactionNormalizer(String appName) {
        Normalizer normalizer = findTransactionNormalizer(appName);
        if (normalizer != null) {
            return normalizer;
        }
        normalizer = createTransactionNormalizer(appName, EMPTY_RULES,
                Collections.<TransactionSegmentTerms> emptyList());
        Normalizer oldNormalizer = transactionNormalizers.putIfAbsent(appName, normalizer);
        return oldNormalizer == null ? normalizer : oldNormalizer;
    }

    private Normalizer findTransactionNormalizer(String appName) {
        if (!autoAppNamingEnabled || appName == null || appName.equals(defaultAppName)) {
            return defaultTransactionNormalizer;
        }
        return transactionNormalizers.get(appName);
    }

    private void replaceTransactionNormalizer(String appName, Normalizer normalizer) {
        Normalizer oldNormalizer = getTransactionNormalizer(appName);
        if (oldNormalizer == defaultTransactionNormalizer) {
            defaultTransactionNormalizer = normalizer;
        } else {
            transactionNormalizers.put(appName, normalizer);
        }
    }

    private Normalizer getOrCreateMetricNormalizer(String appName) {
        Normalizer normalizer = findMetricNormalizer(appName);
        if (normalizer != null) {
            return normalizer;
        }
        normalizer = createMetricNormalizer(appName, EMPTY_RULES);
        Normalizer oldNormalizer = metricNormalizers.putIfAbsent(appName, normalizer);
        return oldNormalizer == null ? normalizer : oldNormalizer;
    }

    private Normalizer findMetricNormalizer(String appName) {
        if (!autoAppNamingEnabled || appName == null || appName.equals(defaultAppName)) {
            return defaultMetricNormalizer;
        }
        return metricNormalizers.get(appName);
    }

    private void replaceMetricNormalizer(String appName, Normalizer normalizer) {
        Normalizer oldNormalizer = getMetricNormalizer(appName);
        if (oldNormalizer == defaultMetricNormalizer) {
            defaultMetricNormalizer = normalizer;
        } else {
            metricNormalizers.put(appName, normalizer);
        }
    }

    private Normalizer createUrlNormalizer(String appName, List<NormalizationRule> urlRules) {
        return NormalizerFactory.createUrlNormalizer(appName, urlRules);
    }

    private Normalizer createTransactionNormalizer(String appName, List<NormalizationRule> metricNameRules,
            List<TransactionSegmentTerms> transactionSegmentTermRules) {
        return NormalizerFactory.createTransactionNormalizer(appName, metricNameRules, transactionSegmentTermRules);
    }

    private Normalizer createMetricNormalizer(String appName, List<NormalizationRule> metricNameRules) {
        return NormalizerFactory.createMetricNormalizer(appName, metricNameRules);
    }

}
