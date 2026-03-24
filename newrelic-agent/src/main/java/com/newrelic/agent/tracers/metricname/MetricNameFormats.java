/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.util.Strings;

public class MetricNameFormats {

    /**
     * Cache for MetricNameFormat instances. Uses AgentCollectionFactory to automatically
     * select correct Caffeine version:
     * - Java 8-10: Uses Caffeine 2.9.3
     * - Java 11+:  Uses Caffeine 3.2.3 (no Unsafe)
     */
    private static final Map<MNFKey, MetricNameFormat> cmsToMnf =
        AgentBridge.collectionFactory.createCacheWithInitialCapacity(16);

    private MetricNameFormats() {
    }

    public static MetricNameFormat replaceFirstSegment(MetricNameFormat metricName, String newSegmentName) {
        String metricNameString = metricName.getMetricName();
        String txName = metricName.getTransactionSegmentName();

        String newTxName;
        String newMetricName = replaceFirstSegment(metricNameString, newSegmentName);
        if (metricNameString.equals(txName)) {
            newTxName = newMetricName;
        } else {
            newTxName = replaceFirstSegment(txName, newSegmentName);
        }
        return new SimpleMetricNameFormat(newMetricName, newTxName);
    }

    private static String replaceFirstSegment(String name, String newSegmentName) {
        String[] segments = name.split(MetricNames.SEGMENT_DELIMITER_STRING);
        segments[0] = newSegmentName;
        return Strings.join(MetricNames.SEGMENT_DELIMITER, segments);
    }
    
    public static MetricNameFormat getFormatter(final Object invocationTarget, final ClassMethodSignature sig) {
        if (sig == null) {
            return null;
        }

        final MNFKey key = new MNFKey(sig, invocationTarget, null, 0);
        return cmsToMnf.computeIfAbsent(key, k -> new ClassMethodMetricNameFormat(sig, key.invocationTargetClassName));
    }

    public static MetricNameFormat getFormatter(final Object invocationTarget, final ClassMethodSignature sig,
            final String metricName, final int flags) {
        if (sig == null) {
            return null;
        }

        final MNFKey key = new MNFKey(sig, invocationTarget, metricName, flags);
        return cmsToMnf.computeIfAbsent(
                key,
                k -> metricName == null
                        ? sig.getMetricNameFormat(key.invocationTargetClassName, flags)
                        : new SimpleMetricNameFormat(getTracerMetricName(key.invocationTargetClassName, sig.getClassName(), metricName)));
    }

    private static final Pattern METRIC_NAME_REPLACE = Pattern.compile("${className}", Pattern.LITERAL);

    private static String getTracerMetricName(String invocationTargetClassName, String className, String metricName) {
        Matcher matcher = METRIC_NAME_REPLACE.matcher(metricName);

        return matcher.replaceFirst(Matcher.quoteReplacement(invocationTargetClassName == null ? className
                : invocationTargetClassName));
    }

    private static class MNFKey {
        final ClassMethodSignature sig;
        final String invocationTargetClassName;
        final String metricName;
        final int flags;

        MNFKey(ClassMethodSignature sig, Object invocationTarget, String metricName, int flags) {
            this.sig = sig;
            this.invocationTargetClassName = invocationTarget != null ? invocationTarget.getClass().getName() : null;
            this.metricName = metricName;
            this.flags = flags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            MNFKey mnfKey = (MNFKey) o;

            if (flags != mnfKey.flags)
                return false;
            if (!sig.equals(mnfKey.sig))
                return false;
            if (invocationTargetClassName != null ?
                    !invocationTargetClassName.equals(mnfKey.invocationTargetClassName) :
                    mnfKey.invocationTargetClassName != null)
                return false;
            return metricName != null ? metricName.equals(mnfKey.metricName) : mnfKey.metricName == null;
        }

        @Override
        public int hashCode() {
            int result = sig.hashCode();
            result = 31 * result + (invocationTargetClassName != null ? invocationTargetClassName.hashCode() : 0);
            result = 31 * result + (metricName != null ? metricName.hashCode() : 0);
            result = 31 * result + flags;
            return result;
        }
    }
}
