/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.language;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.samplers.NoopSamplerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.util.Map;

import static com.newrelic.agent.language.SourceLanguageService.resolveExtension;
import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceLanguageServiceTest {
    private static final String SUPPORTABILITY_METRIC_FOR_CLASS = format(MetricNames.SUPPORTABILITY_LOADED_CLASSES_SOURCE_VERSION, "java", "1.8");

    private SourceLanguageService sourceLanguageService;

    @BeforeClass
    public static void setup() {
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setHarvestService(new MockHarvestService());
        serviceManager.setSamplerService(new NoopSamplerService());
        ServiceFactory.setServiceManager(serviceManager);
        try {
            serviceManager.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void cleanup() throws Exception {
        sourceLanguageService.stop();
    }

    @Before
    public void beforeTest() throws Exception {
        AgentHelper.initializeConfig();
        sourceLanguageService = new SourceLanguageService();
        ((MockServiceManager) ServiceFactory.getServiceManager()).setSourceLanguageService(sourceLanguageService);
        sourceLanguageService.start();
    }

    @Test
    public void extensionResolution() {
        assertEquals("unknown", resolveExtension(null));
        assertEquals("unknown", resolveExtension("test"));

        assertEquals("unknown", resolveExtension("test.exe"));
        assertEquals("unknown", resolveExtension("test.adb")); // ada
        assertEquals("unknown", resolveExtension("test.cbl")); // cobol
        assertEquals("unknown", resolveExtension("test.cfm")); // cfm
        assertEquals("unknown", resolveExtension("test.lisp")); // LISP
        assertEquals("unknown", resolveExtension("test.pas")); // Pascal
        assertEquals("unknown", resolveExtension("test.pro")); // Prolog

        assertEquals("java", resolveExtension("StrictMath.java"));

        assertEquals("scala", resolveExtension("ClojureTest.sc"));
        assertEquals("scala", resolveExtension("ClojureTest.scala"));

        assertEquals("clojure", resolveExtension("ClojureTest.clj"));
        assertEquals("clojure", resolveExtension("ClojureTest.cljs"));
        assertEquals("clojure", resolveExtension("ClojureTest.cljc"));
        assertEquals("clojure", resolveExtension("ClojureTest.edn"));

        assertEquals("kotlin", resolveExtension("KotlinTest.kt"));
        assertEquals("kotlin", resolveExtension("KotlinTest.kts"));
    }

    @Test
    public void classExtensionAndVersionRecordedOnVisit() throws Exception {
        readClass(this.getClass());
        Map<String, Long> sourceCounts = sourceLanguageService.getSourceCounts();
        assertTrue(sourceCounts.containsKey(SUPPORTABILITY_METRIC_FOR_CLASS));
        assertEquals(1L, sourceCounts.get(SUPPORTABILITY_METRIC_FOR_CLASS).longValue());
    }

    @Test
    public void countsResetToZeroAfterHarvest() throws Exception {
        readClass(this.getClass());
        Map<String, Long> sourceCounts = sourceLanguageService.getSourceCounts();
        assertTrue(sourceCounts.containsKey(SUPPORTABILITY_METRIC_FOR_CLASS));
        assertEquals(1L, sourceCounts.get(SUPPORTABILITY_METRIC_FOR_CLASS).longValue());

        for (Map.Entry<String, Long> entry : sourceCounts.entrySet()) {
            if (!SUPPORTABILITY_METRIC_FOR_CLASS.equals(entry.getKey())) {
                assertEquals(0, entry.getValue().longValue());
            }
        }

        StatsEngineImpl statsEngine = new StatsEngineImpl();
        sourceLanguageService.beforeHarvest("Unit Test", statsEngine);
        sourceLanguageService.afterHarvest("Unit Test");
        assertEquals(1, statsEngine.getResponseTimeStats(SUPPORTABILITY_METRIC_FOR_CLASS).getCallCount());

        // all entries in the map should be reset
        for (Map.Entry<String, Long> entry : sourceLanguageService.getSourceCounts().entrySet()) {
            assertEquals(0, entry.getValue().longValue());
        }
    }

    public void readClass(Class<?> clazz) throws Exception {
        ClassReader classReader = new ClassReader(this.getClass().getName());
        ClassVisitor visitor = sourceLanguageService
                .getSourceVisitor()
                .newClassMatchVisitor(null, this.getClass(), classReader, null, null);
        classReader.accept(visitor, ClassReader.SKIP_CODE);
    }
}