/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.agent.instrumentation.solr;

import com.codahale.metrics.Meter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SolrComponentRegistryTest {

    @Before
    public void setUp() {
        SolrComponentRegistry.clear();
        MetricUtil.clearAll();
    }

    @Test
    public void testRegisterAndGetNameForTag() {
        String tag = "SolrCore@123:DirectUpdateHandler2@456";
        String name = "org.apache.solr.update.DirectUpdateHandler2";
        SolrComponentRegistry.registerComponent(tag, name);

        assertEquals(name, SolrComponentRegistry.getNameForTag(tag));
    }

    @Test
    public void testGetNameForTagReturnsNullForUnknownTag() {
        String unknownTag = "unknown-tag";
        String result = SolrComponentRegistry.getNameForTag(unknownTag);

        assertNull(result);
    }

    @Test
    public void testFullyQualifiedClassNameUpdatesMatchingMetrics() {
        String tag = "SolrCore@123:DirectUpdateHandler2@456";
        String fallbackName = "updateHandler";
        String qualifiedName = "org.apache.solr.update.DirectUpdateHandler2";

        Meter meter = new Meter();
        MeteredMetric metric = new MeteredMetric("commits", "updateHandler", "testRegistry", fallbackName, meter, tag);
        MetricUtil.addMetric(metric);

        assertEquals(fallbackName, metric.getName());

        SolrComponentRegistry.registerComponent(tag, qualifiedName);

        assertEquals(qualifiedName, metric.getName());
    }

    @Test
    public void testOnlyMetricsWithMatchingTagAreUpdated() {
        String tag1 = "SolrCore@123:DirectUpdateHandler2@456";
        String tag2 = "SolrCore@789:UpdateRequestHandler@012";
        String qualifiedName = "org.apache.solr.update.DirectUpdateHandler2";

        Meter meter1 = new Meter();
        Meter meter2 = new Meter();
        MeteredMetric metric1 = new MeteredMetric("commits", "updateHandler", "testRegistry1", "updateHandler", meter1, tag1);
        MeteredMetric metric2 = new MeteredMetric("requests", "updateHandler", "testRegistry2", "updateHandler", meter2, tag2);
        MetricUtil.addMetric(metric1);
        MetricUtil.addMetric(metric2);

        SolrComponentRegistry.registerComponent(tag1, qualifiedName);

        assertEquals(qualifiedName, metric1.getName());
        assertEquals("updateHandler", metric2.getName()); // Should remain unchanged
    }

    @Test
    public void testMetricsWithDotsAreNotUpdated() {
        String tag = "SolrCore@123:UpdateRequestHandler@456";
        String existingQualifiedName = "org.apache.solr.update.DirectUpdateHandler2";
        String newQualifiedName = "org.apache.solr.handler.UpdateRequestHandler";

        Meter meter = new Meter();
        MeteredMetric metric = new MeteredMetric("commits", "updateHandler", "testRegistry", existingQualifiedName, meter, tag);
        MetricUtil.addMetric(metric);

        SolrComponentRegistry.registerComponent(tag, newQualifiedName);

        assertEquals(existingQualifiedName, metric.getName());
    }

    @Test
    public void testSimpleNamesDoNotTriggerUpdates() {
        String tag = "SolrCore@123:SpellCheckComponent@456";
        String simpleName = "spellcheck"; // Not a fully qualified class name

        Meter meter = new Meter();
        MeteredMetric metric = new MeteredMetric("requests", "handler", "testRegistry", "handler", meter, tag);
        MetricUtil.addMetric(metric);

        SolrComponentRegistry.registerComponent(tag, simpleName);

        assertEquals("handler", metric.getName());
    }

    @Test
    public void testCacheMetricsAreNotAffectedByHandlerRegistration() {
        String handlerTag = "SolrCore@123:UpdateRequestHandler@456";
        String cacheTag = "SolrCore@123:SolrIndexSearcher@789:CaffeineCache@012";
        String handlerName = "org.apache.solr.handler.UpdateRequestHandler";

        Meter meter = new Meter();
        MeteredMetric cacheMetric = new MeteredMetric("hits", "filterCache", "testRegistry", "filterCache", meter, cacheTag);
        MetricUtil.addMetric(cacheMetric);

        SolrComponentRegistry.registerComponent(handlerTag, handlerName);

        assertEquals("filterCache", cacheMetric.getName());
    }

    @Test
    public void testMultipleMetricsWithSameTagAllGetUpdated() {
        String tag = "SolrCore@123:DirectUpdateHandler2@456";
        String qualifiedName = "org.apache.solr.update.DirectUpdateHandler2";

        Meter meter1 = new Meter();
        Meter meter2 = new Meter();
        Meter meter3 = new Meter();
        MeteredMetric metric1 = new MeteredMetric("commits", "updateHandler", "testRegistry1", "updateHandler", meter1, tag);
        MeteredMetric metric2 = new MeteredMetric("rollbacks", "updateHandler", "testRegistry2", "updateHandler", meter2, tag);
        MeteredMetric metric3 = new MeteredMetric("optimizes", "updateHandler", "testRegistry3", "updateHandler", meter3, tag);
        MetricUtil.addMetric(metric1);
        MetricUtil.addMetric(metric2);
        MetricUtil.addMetric(metric3);

        SolrComponentRegistry.registerComponent(tag, qualifiedName);

        assertEquals(qualifiedName, metric1.getName());
        assertEquals(qualifiedName, metric2.getName());
        assertEquals(qualifiedName, metric3.getName());
    }

    @Test
    public void testMetricsWithNullTagAreNotUpdated() {
        String tag = "SolrCore@123:DirectUpdateHandler2@456";
        String qualifiedName = "org.apache.solr.update.DirectUpdateHandler2";

        Meter meter = new Meter();
        MeteredMetric metric = new MeteredMetric("commits", "updateHandler", "testRegistry", "updateHandler", meter, null);
        MetricUtil.addMetric(metric);

        SolrComponentRegistry.registerComponent(tag, qualifiedName);

        assertEquals("updateHandler", metric.getName());
    }

    @Test
    public void testClearRemovesAllMappings() {
        String tag1 = "tag1";
        String tag2 = "tag2";
        SolrComponentRegistry.registerComponent(tag1, "org.example.Class1");
        SolrComponentRegistry.registerComponent(tag2, "org.example.Class2");

        SolrComponentRegistry.clear();

        assertNull(SolrComponentRegistry.getNameForTag(tag1));
        assertNull(SolrComponentRegistry.getNameForTag(tag2));
    }

    @Test
    public void testRegisterComponentWithNullValues() {
        // No exceptions thrown...
        SolrComponentRegistry.registerComponent(null, "someName");
        SolrComponentRegistry.registerComponent("someTag", null);
        SolrComponentRegistry.registerComponent(null, null);
    }

    @Test
    public void testUpdatingExistingComponentMapping() {
        String tag = "SolrCore@123:Handler@456";
        String oldName = "org.example.OldHandler";
        String newName = "org.example.NewHandler";

        SolrComponentRegistry.registerComponent(tag, oldName);
        assertEquals(oldName, SolrComponentRegistry.getNameForTag(tag));

        SolrComponentRegistry.registerComponent(tag, newName);

        assertEquals(newName, SolrComponentRegistry.getNameForTag(tag));
    }
}