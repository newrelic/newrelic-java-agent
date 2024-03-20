package com.newrelic.agent.jmx.values;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;

import java.util.ArrayList;
import java.util.List;

public class NonIteratedSolr7JmxValues extends JmxFrameworkValues {

    public static final String PREFIX = "solr7";

    private static final int METRIC_COUNT = 4;
    private static final List<BaseJmxValue> METRICS = new ArrayList<>(METRIC_COUNT);

    /* Starting in Solr 7 the mbean naming hierarchy was changed.
     * The Solr7JmxValues class was added to capture the new metrics for 7+.
     * We look for the new metric, then we just needed to change the metric name (pObjectMetricName) that we send to NR so that it conforms
     * to the previous metric format (which is hardcoded in rpm_site) instead of sending up the metric that the agent would create
     * by default.
     * However, the actual attribute names for the updateHandler scope (in UPDATE category) changed and the rpm_site
     * regex for that metric did not match the new attributes because of the way the agent
     * appends the attribute name to pObjectMetricName. In conjunction, a change in rpm_site was made that
     * will capture the new attribute names for solr7+ update category metrics.
     */

    static {

        METRICS.add(new BaseJmxValue(
                "solr:dom1=core,dom2=*,category=CACHE,scope=searcher,name=queryResultCache",
                "JMX/solr/{dom2}/queryResultCache/%/",
                createCacheMetrics()
        ));

        METRICS.add(new BaseJmxValue(
                "solr:dom1=core,dom2=*,category=CACHE,scope=searcher,name=filterCache",
                "JMX/solr/{dom2}/filterCache/%/",
                createCacheMetrics()
        ));

        METRICS.add(new BaseJmxValue(
                "solr:dom1=core,dom2=*,category=CACHE,scope=searcher,name=documentCache",
                "JMX/solr/{dom2}/documentCache/%/",
                createCacheMetrics()
        ));

        METRICS.add(new BaseJmxValue(
                "solr:dom1=core,dom2=*,category=UPDATE,scope=updateHandler,name=*",
                "JMX/solr/{dom2}/updateHandler/%/{name}",
                new JmxMetric[] {
                        JmxMetric.create("Value", JmxType.SIMPLE),
                        JmxMetric.create("RateUnit", JmxType.SIMPLE),
                        JmxMetric.create("FiveMinuteRate", JmxType.SIMPLE),
                        JmxMetric.create("FifteenMinuteRate", JmxType.SIMPLE),
                        JmxMetric.create("MeanRate", JmxType.SIMPLE),
                        JmxMetric.create("OneMinuteRate", JmxType.SIMPLE),
                        JmxMetric.create("Count", JmxType.MONOTONICALLY_INCREASING) }
        ));

    }

    public NonIteratedSolr7JmxValues() {
        super();
    }

    private static JmxMetric[] createCacheMetrics() {

        return new JmxMetric[] {
                JmxMetric.create("size", JmxType.SIMPLE),
                JmxMetric.create("hitratio", JmxType.SIMPLE), JmxMetric.create("size", JmxType.SIMPLE),
                JmxMetric.create("cumulative_hitratio", JmxType.SIMPLE),
                JmxMetric.create("lookups", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("hits", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("inserts", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("evictions", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("cumulative_lookups", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("cumulative_hits", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("cumulative_inserts", JmxType.MONOTONICALLY_INCREASING),
                JmxMetric.create("cumulative_evictions", JmxType.MONOTONICALLY_INCREASING)
        };
    }

    @Override
    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    @Override
    public String getPrefix() { return PREFIX; }
}
