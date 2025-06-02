/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageLifetimeListener;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * Hooks into the Weaver's lifetime listener. Logs events, generates supportability metrics, and manages closeables.
 */
public class AgentWeaverListener implements WeavePackageLifetimeListener {
    private final ConcurrentMap<WeavePackage, List<Closeable>> closeables = new ConcurrentHashMap<>();
    private final WeaveViolationLogger weaveViolationLogger;

    public AgentWeaverListener(WeaveViolationLogger weaveViolationLogger) {
        this.weaveViolationLogger = weaveViolationLogger;
    }

    @Override
    public void registered(WeavePackage weavepackage) {
        Agent.LOG.log(Level.FINE, "registered weave package: {0}", weavepackage.getName());
        closeables.put(weavepackage, new ArrayList<Closeable>());
    }

    @Override
    public void deregistered(WeavePackage weavepackage) {
        Agent.LOG.log(Level.FINE, "deregistered weave package: {0}", weavepackage.getName());
        List<Closeable> closers = closeables.remove(weavepackage);
        Agent.LOG.log(Level.FINER, "{0}: closing {1} closeables", weavepackage.getName(), closers.size());
        for (Closeable closer : closers) {
            try {
                Agent.LOG.log(Level.FINER, "\t{0}", closer);
                closer.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void validated(PackageValidationResult packageResult, ClassLoader classloader) {
        final String weavePackageName = packageResult.getWeavePackage().getName();
        final float weavePackageVersion = packageResult.getWeavePackage().getVersion();
        if (packageResult.succeeded()) {
            String supportabilityLoadedMetric;
            if (packageResult.getWeavePackage().getConfig().isCustom()) {
                supportabilityLoadedMetric = MetricNames.SUPPORTABILITY_WEAVE_CUSTOM_LOADED;
            } else {
                supportabilityLoadedMetric = MetricNames.SUPPORTABILITY_WEAVE_LOADED;
            }
            ServiceFactory.getStatsService().doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat.format(
                    supportabilityLoadedMetric, weavePackageName, weavePackageVersion), 1), supportabilityLoadedMetric);
            Agent.LOG.log(Level.FINE, "{0} - validated classloader {1}", weavePackageName, classloader);
        } else {
            WeavePackage weavePackage = packageResult.getWeavePackage();
            if (Agent.LOG.isFinestEnabled() && weavePackage.weavesBootstrap()) {
                Map<String, MatchType> matchTypes = weavePackage.getMatchTypes();
                for (Map.Entry<String, MatchType> entry : matchTypes.entrySet()) {
                    if (entry.getValue() != null) {
                        Agent.LOG.log(Level.FINEST, "Bootstrap class {0} : {1}", entry.getKey(),
                                weavePackage.isBootstrapClassName(Collections.singleton(entry.getKey()))); // FIXME ???
                    }
                }
            }
            
            boolean isCustom = weavePackage.getConfig().isCustom();
            String supportabilitySkippedMetric = isCustom ? MetricNames.SUPPORTABILITY_WEAVE_CUSTOM_SKIPPED : MetricNames.SUPPORTABILITY_WEAVE_SKIPPED;

            ServiceFactory.getStatsService().doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat.format(
                    supportabilitySkippedMetric, weavePackageName, weavePackageVersion), 1), supportabilitySkippedMetric);

            weaveViolationLogger.logWeaveViolations(packageResult, classloader, isCustom);
        }
    }

    public void registerInstrumentationCloseable(String weavePackageName, WeavePackage weavePackage, Closeable closeable) {
        if (null != weavePackage && closeables.containsKey(weavePackage)) {
            closeables.get(weavePackage).add(closeable);
        } else {
            Agent.LOG.log(
                    Level.FINER,
                    "Asked to register closeable for weave package {0} but no such package exists. Closing {1}. This should rarely happen.",
                    weavePackageName, closeable);
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
