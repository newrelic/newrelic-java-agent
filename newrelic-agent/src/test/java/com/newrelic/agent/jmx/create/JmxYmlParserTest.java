/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import static com.newrelic.agent.AgentHelper.getFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.jmx.JmxType;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Tests the utility methods found in JmxYmlUtils.
 * 
 * @since Mar 5, 2013
 */
public class JmxYmlParserTest {

    private static final String SOLR_TEST_PATH = "com/newrelic/agent/jmx/create/solr_test.yml";
    private static final String NO_JMX_TEST_PATH = "com/newrelic/agent/jmx/create/no_jmx.yml";
    private static final String INVALID_METRICS_TEST_PATH = "com/newrelic/agent/jmx/create/invalid_metric_attributes.yml";

    @Test
    public void testSampleSolrFile() throws Exception {
        File theFile = getFile(SOLR_TEST_PATH);
        Config input = readYml(theFile);
        Object jmx = input.getProperty("jmx");
        Assert.assertNotNull(jmx);
        List<Map> jmxConfigs = (List<Map>) jmx;
        Assert.assertEquals(7, jmxConfigs.size());

        /*
         * - object_name: solr*:type=queryResultCache,* metrics: - attributes: lookups, hits, inserts, evictions,
         * cumulative_lookups, cumulative_hits, cumulative_inserts, cumulative_evictions type: monotonically_increasing
         * - attributes: hitratio, size, cumulative_hitratio
         */
        Map jmxConfig = jmxConfigs.get(0);
        JmxYmlParser parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(true, parser.getEnabled());
        Assert.assertEquals("solr*:type=queryResultCache,*", parser.getObjectName());
        Map<JmxType, List<String>> attrs = parser.getAttrs();
        Assert.assertNotNull(attrs);
        List<String> simpleAtts = attrs.get(JmxType.SIMPLE);
        Assert.assertNull(simpleAtts);
        List<String> monoAtts = attrs.get(JmxType.MONOTONICALLY_INCREASING);
        Assert.assertNotNull(monoAtts);
        Assert.assertEquals(11, monoAtts.size());
        Assert.assertTrue(monoAtts.contains("lookups"));
        Assert.assertTrue(monoAtts.contains("hits"));
        Assert.assertTrue(monoAtts.contains("inserts"));
        Assert.assertTrue(monoAtts.contains("cumulative_inserts"));
        Assert.assertTrue(monoAtts.contains("cumulative_evictions"));
        Assert.assertTrue(monoAtts.contains("hitratio"));
        Assert.assertTrue(monoAtts.contains("size"));
        Assert.assertTrue(monoAtts.contains("cumulative_hitratio"));

        /*
         * - object_name: solr*:type=filterCache,* metrics: - attributes: lookups, hits, inserts, evictions,
         * cumulative_lookups, cumulative_hits, cumulative_inserts, cumulative_evictions - attributes: hitratio, size,
         * cumulative_hitratio type: simple
         */
        jmxConfig = jmxConfigs.get(1);
        parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(true, parser.getEnabled());
        Assert.assertEquals("solr*:type=filterCache,*", parser.getObjectName());
        attrs = parser.getAttrs();
        Assert.assertNotNull(attrs);
        simpleAtts = attrs.get(JmxType.SIMPLE);
        Assert.assertNotNull(simpleAtts);
        Assert.assertEquals(3, simpleAtts.size());
        Assert.assertTrue(simpleAtts.contains("hitratio"));
        Assert.assertTrue(simpleAtts.contains("size"));
        Assert.assertTrue(simpleAtts.contains("cumulative_hitratio"));
        monoAtts = attrs.get(JmxType.MONOTONICALLY_INCREASING);
        Assert.assertNotNull(monoAtts);
        Assert.assertEquals(8, monoAtts.size());
        Assert.assertTrue(monoAtts.contains("lookups"));
        Assert.assertTrue(monoAtts.contains("hits"));
        Assert.assertTrue(monoAtts.contains("inserts"));
        Assert.assertTrue(monoAtts.contains("evictions"));
        Assert.assertTrue(monoAtts.contains("cumulative_lookups"));

        /*
         * - object_name: solr*:type=documentCache,* metrics:
         */
        jmxConfig = jmxConfigs.get(2);
        parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(true, parser.getEnabled());
        Assert.assertEquals("solr*:type=documentCache,*", parser.getObjectName());
        attrs = parser.getAttrs();
        Assert.assertNull(attrs);

        /*
         * - object_name: solr*:type==empty,*
         */
        jmxConfig = jmxConfigs.get(3);
        parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(true, parser.getEnabled());
        Assert.assertEquals("solr*:type==empty,*", parser.getObjectName());
        attrs = parser.getAttrs();
        Assert.assertNull(attrs);

        /*
         * - object_name: solr*:type=updateHandler,* metrics: - attribute: docsPending type: simple - attributes:
         * expungeDeletes, rollbacks, optimizes, autocommits, commits, errors, adds, deletesById, deletesByQuery -
         * attributes: cumulative_adds, cumulative_deletesById, cumulative_deletesByQuery, cumulative_errors
         */
        jmxConfig = jmxConfigs.get(4);
        parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(true, parser.getEnabled());
        Assert.assertEquals("solr*:type=updateHandler,*", parser.getObjectName());
        attrs = parser.getAttrs();
        Assert.assertNotNull(attrs);
        simpleAtts = attrs.get(JmxType.SIMPLE);
        Assert.assertNotNull(simpleAtts);
        Assert.assertEquals(1, simpleAtts.size());
        monoAtts = attrs.get(JmxType.MONOTONICALLY_INCREASING);
        Assert.assertNotNull(monoAtts);
        Assert.assertEquals(13, monoAtts.size());
        Assert.assertTrue(monoAtts.contains("expungeDeletes"));
        Assert.assertTrue(monoAtts.contains("rollbacks"));
        Assert.assertTrue(monoAtts.contains("optimizes"));
        Assert.assertTrue(monoAtts.contains("autocommits"));
        Assert.assertTrue(monoAtts.contains("commits"));
        Assert.assertTrue(monoAtts.contains("errors"));
        Assert.assertTrue(monoAtts.contains("adds"));
        Assert.assertTrue(monoAtts.contains("deletesById"));
        Assert.assertTrue(monoAtts.contains("deletesByQuery"));
        Assert.assertTrue(monoAtts.contains("cumulative_adds"));
        Assert.assertTrue(monoAtts.contains("cumulative_deletesByQuery"));
        Assert.assertTrue(monoAtts.contains("cumulative_deletesById"));
        Assert.assertTrue(monoAtts.contains("cumulative_errors"));

        jmxConfig = jmxConfigs.get(5);
        parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(false, parser.getEnabled());
        Assert.assertEquals("solr*:type=yay", parser.getObjectName());

        jmxConfig = jmxConfigs.get(6);
        parser = new JmxYmlParser(jmxConfig);
        Assert.assertEquals(true, parser.getEnabled());
        Assert.assertEquals("solr*:type=rara", parser.getObjectName());
    }

    @Test
    public void testFileWithNoJmx() throws Exception {
        File theFile = getFile(NO_JMX_TEST_PATH);
        Config input = readYml(theFile);
        Object jmx = input.getProperty("jmx");
        Assert.assertNull(jmx);
    }

    @Test
    public void testFileWithInvalidMetricAttributes() throws Exception {
        File theFile = getFile(INVALID_METRICS_TEST_PATH);
        Config input = readYml(theFile);
        Object jmx = input.getProperty("jmx");
        Assert.assertNotNull(jmx);
        List<Map> jmxConfigs = (List<Map>) jmx;
        Map jmxConfig = jmxConfigs.get(0);

        JmxYmlParser parser = new JmxYmlParser(jmxConfig);
        Map<JmxType, List<String>> attrs = parser.getAttrs();
        Assert.assertNull(attrs);
    }

    protected static BaseConfig readYml(File file) throws Exception {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (Reader reader = new FileReader(file)) {
            Map output = (Map) yaml.load(reader);
            return new BaseConfig(output);
        }
    }

}
