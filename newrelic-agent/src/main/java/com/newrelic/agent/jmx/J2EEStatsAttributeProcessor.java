/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.management.Attribute;
import javax.management.ObjectInstance;
import javax.management.j2ee.statistics.BoundaryStatistic;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.JCAConnectionPoolStats;
import javax.management.j2ee.statistics.JCAConnectionStats;
import javax.management.j2ee.statistics.JCAStats;
import javax.management.j2ee.statistics.JDBCConnectionPoolStats;
import javax.management.j2ee.statistics.JDBCConnectionStats;
import javax.management.j2ee.statistics.JDBCStats;
import javax.management.j2ee.statistics.JMSConnectionStats;
import javax.management.j2ee.statistics.JMSSessionStats;
import javax.management.j2ee.statistics.JMSStats;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;
import javax.management.j2ee.statistics.TimeStatistic;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.StatsEngine;

public class J2EEStatsAttributeProcessor extends AbstractStatsAttributeProcessor {

    // don't remove. this is called by the JMXService through reflection
    public J2EEStatsAttributeProcessor() {

    }

    @Override
    public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName,
            Map<String, Float> values) {
        Object value = attribute.getValue();
        if (value instanceof Stats) {
            boolean isBuiltInMetric = isBuiltInMetric(metricName);
            if (value instanceof JDBCStats) {
                pullJDBCStats(statsEngine, (JDBCStats) value, attribute, metricName, values, isBuiltInMetric);
            } else if (value instanceof JCAStats) {
                pullJCAStats(statsEngine, (JCAStats) value, attribute, metricName, values, isBuiltInMetric);
            } else if (value instanceof JMSStats) {
                pullJMSStats(statsEngine, (JMSStats) value, attribute, metricName, values, isBuiltInMetric);
            } else {
                Stats jmxStats = (Stats) value;
                grabBaseStats(statsEngine, jmxStats, attribute, metricName, values, isBuiltInMetric);
            }
            return true;
        } else {
            Agent.LOG.finer(MessageFormat.format(
                    "Attribute value is not a javax.management.j2ee.statistics.Stats: {0}", value.getClass().getName()));
            return false;
        }
    }

    private static void pullJMSStats(StatsEngine statsEngine, JMSStats jmsStats, Attribute attribute,
            String metricName, Map<String, Float> values, boolean isBuiltInMetric) {
        for (JMSConnectionStats connStats : jmsStats.getConnections()) {
            for (JMSSessionStats current : connStats.getSessions()) {
                grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
            }
        }
    }

    private static void pullJDBCStats(StatsEngine statsEngine, JDBCStats jdbcStats, Attribute attribute,
            String metricName, Map<String, Float> values, boolean isBuiltInMetric) {

        if (jdbcStats.getConnectionPools() != null) {
            for (JDBCConnectionPoolStats current : jdbcStats.getConnectionPools()) {
                grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
            }
        }
        if (jdbcStats.getConnections() != null) {
            for (JDBCConnectionStats current : jdbcStats.getConnections()) {
                grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
            }
        }
    }

    private static void pullJCAStats(StatsEngine statsEngine, JCAStats jcaStats, Attribute attribute,
            String metricName, Map<String, Float> values, boolean isBuiltInMetric) {

        if (jcaStats.getConnectionPools() != null) {
            for (JCAConnectionPoolStats current : jcaStats.getConnectionPools()) {
                grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
            }
        }
        if (jcaStats.getConnections() != null) {
            for (JCAConnectionStats current : jcaStats.getConnections()) {
                grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
            }
        }
    }

    private static void grabBaseStats(StatsEngine statsEngine, Stats jmxStats, Attribute attribute, String metricName,
            Map<String, Float> values, boolean isBuiltInMetric) {
        for (Statistic statistic : jmxStats.getStatistics()) {
            if (isBuiltInMetric) {
                // this will only pull the desired metric from the stats object
                if (addJmxValue(attribute, statistic, values)) {
                    break;
                }
            } else {
                // this will pull all metrics from the stats object
                processStatistic(statsEngine, metricName, attribute, statistic);
            }
        }
    }

    static void processStatistic(StatsEngine statsEngine, String metricName, Attribute attribute, Statistic statistic) {
        String fullMetricName = metricName + '/' + statistic.getName();
        Agent.LOG.finer(MessageFormat.format("Processing J2EE statistic: {0} class: {1}", statistic.getName(),
                statistic.getClass().getName()));
        if (statistic instanceof CountStatistic) {
            CountStatistic stat = (CountStatistic) statistic;
            statsEngine.getStats(fullMetricName).recordDataPoint(stat.getCount());
        } else if (statistic instanceof RangeStatistic) {
            RangeStatistic stat = (RangeStatistic) statistic;
            statsEngine.getStats(fullMetricName).recordDataPoint(stat.getCurrent());
        } else if (statistic instanceof BoundaryStatistic) {
            BoundaryStatistic stat = (BoundaryStatistic) statistic;
            statsEngine.getStats(fullMetricName).recordDataPoint(stat.getLowerBound());
            statsEngine.getStats(fullMetricName).recordDataPoint(stat.getUpperBound());
        } else if (statistic instanceof TimeStatistic) {
            TimeStatistic stat = (TimeStatistic) statistic;
            TimeUnit unit = getTimeUnit(stat.getUnit());
            statsEngine.getResponseTimeStats(fullMetricName).recordResponseTime((int) stat.getCount(),
                    stat.getTotalTime(), stat.getMinTime(), stat.getMaxTime(), unit);
        } else {
            Agent.LOG.log(Level.FINEST, "Not supported: {0}", statistic.getClass().getName());
        }
        Agent.LOG.finer(MessageFormat.format("Processed J2EE statistic: {0} att: {1}", fullMetricName,
                statistic.getName()));
    }

    /**
     * Return true if the metric was found, meaning we do not need to continue through the rest of the metrics.
     */
    static boolean addJmxValue(Attribute attribute, Statistic statistic, Map<String, Float> values) {
        if (attribute.getName().contains(statistic.getName())) {
            Agent.LOG.finer(MessageFormat.format("Adding J2EE statistic to List: {0} class: {1}", attribute.getName(),
                    statistic.getClass().getName()));
            if (statistic instanceof CountStatistic) {
                CountStatistic stat = (CountStatistic) statistic;
                values.put(attribute.getName(), (float) stat.getCount());
                return true;
            } else if (statistic instanceof RangeStatistic) {
                RangeStatistic stat = (RangeStatistic) statistic;
                values.put(attribute.getName(), (float) stat.getCurrent());
                return true;
            } else if (statistic instanceof BoundaryStatistic) {
                BoundaryStatistic stat = (BoundaryStatistic) statistic;
                values.put(attribute.getName(), (float) ((stat.getLowerBound() + stat.getUpperBound()) / 2));
                return true;
            } else if (statistic instanceof TimeStatistic) {
                TimeStatistic stat = (TimeStatistic) statistic;
                if (stat.getCount() == 0) {
                    values.put(attribute.getName(), 0f);
                } else {
                    values.put(attribute.getName(), (float) (stat.getTotalTime() / stat.getCount()));
                }
                return true;
            }
            Agent.LOG.finer(MessageFormat.format("Added J2EE statistic: {0}", attribute.getName()));

        } else {
            Agent.LOG.log(Level.FINEST, MessageFormat.format("Ignoring stat {0}. Looking for att name {1}.",
                    statistic.getName(), attribute.getName()));
        }
        return false;
    }
}
