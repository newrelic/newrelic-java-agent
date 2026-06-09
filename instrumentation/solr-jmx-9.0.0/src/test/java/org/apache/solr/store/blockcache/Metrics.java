/*
 * Replacement for org.apache.solr.store.blockcache.Metrics for Solr 9.0+
 *
 * The original class was removed when HDFS support was moved to a separate module (SOLR-14660).
 * This is a minimal test implementation that provides the same interface for testing purposes.
 *
 * Original class was in: org.apache.solr.store.blockcache.Metrics
 * Extended: SolrCacheBase
 * Implemented: SolrInfoBean, SolrMetricProducer
 */
package org.apache.solr.store.blockcache;

import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.SolrMetricsContext;

/**
 * A test implementation of SolrInfoBean for testing Solr 9.0+ metrics.
 */
public class Metrics implements SolrInfoBean {
    private static final String NAME = "hdfsBlockCache";
    private static final String DESCRIPTION = "Provides metrics for the HdfsDirectoryFactory BlockCache.";
    private SolrMetricsContext metricsContext;

    public Metrics() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Category getCategory() {
        return Category.CACHE;
    }

    @Override
    public void initializeMetrics(SolrMetricsContext parentContext, String scope) {
        this.metricsContext = parentContext.getChildContext(this);
    }

    @Override
    public SolrMetricsContext getSolrMetricsContext() {
        return metricsContext;
    }

    @Override
    public String toString() {
        return "Metrics{" +
                "name='" + NAME + '\'' +
                ", category=" + Category.CACHE +
                '}';
    }
}
