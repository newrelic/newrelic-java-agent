/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import com.google.common.base.Splitter;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This test is here because the JSP file does not always come in as one chunk. Instead many times it comes in as
 * pieces.
 */
public class RumStreamParserTest {

    private static final String APP_NAME = "Unit Test";

    @BeforeClass
    public static void beforeClass() throws Exception {
        createServiceManager(createConfigMap());
    }

    public static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("apdex_t", 0.5f);
        return map;
    }

    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();

        serviceManager.setNormalizationService(new NormalizationServiceImpl());

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);
        statsService.start();
    }

    public String addHeader(String jsp) throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        TemplateTextImpl node1 = new TemplateTextImpl(jsp);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);
        return node1.toString();
    }

    @Test
    public void checkAllFiles() throws Exception {
        List<File> files = AgentHelper.getFiles("com/newrelic/agent/tracers/jasper/chuncktestfiles");
        if (files == null || files.isEmpty()) {
            Assert.fail("There were no files read in for testing.");
        }
        for (File current : files) {
            processFile(current);
        }

    }

    private void processFile(File current) throws Exception {
        System.out.println("Processing File: " + current);
        String actual = getActualInParts(current);
        System.out.println("Actual:\n" + actual);
        String expected = getExpected(current);
        Assert.assertEquals("The header or footer was not added correct for file: " + current.getName(), expected,
                actual);
    }

    public String getActualInParts(File file) throws Exception {
        String init = getFileString(file, false);
        // chunck the file up into 50 character strings
        Iterable<String> pieces = Splitter.fixedLength(50).split(init);

        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder output = new StringBuilder();
        for (String current : pieces) {
            TemplateTextImpl node1 = new TemplateTextImpl(current);
            GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
            factory.processText(tx, visitor, node1);
            output.append(node1.toString());
        }
        return output.toString();
    }

    public String getExpected(File file) {
        return getFileString(file, true);
    }

    public String getFileString(File file, boolean addScripts) {
        String line;
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                if (addScripts) {
                    String output = checkHeaderLine(line);
                    output = checkFooterLine(output);
                    sb.append(output);
                } else {
                    String output = checkHeaderLineRemove(line);
                    output = checkFooterLineRemove(output);
                    sb.append(output);
                }
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Exception should not have been thrown: " + e.getMessage());
        }
        return null;
    }

    private String checkHeaderLineRemove(String current) {
        return current.replaceAll("EXPECTED_RUM_HEADER_LOCATION", "");
    }

    private String checkFooterLineRemove(String current) {
        return current.replaceAll("EXPECTED_RUM_FOOTER_LOCATION", "");
    }

    private String checkHeaderLine(String current) {
        return current.replaceAll("EXPECTED_RUM_HEADER_LOCATION", AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET);
    }

    private String checkFooterLine(String current) {
        return current.replaceAll("EXPECTED_RUM_FOOTER_LOCATION", AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET);
    }

    private static class TemplateTextImpl implements TemplateText {

        private final String text;
        private final StringBuilder sb = new StringBuilder();

        public TemplateTextImpl(String text) {
            this.text = text;
        }

        @Override
        public String getText() throws Exception {
            return text;
        }

        @Override
        public void setText(String text) throws Exception {
            sb.append(text);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    private static class GenerateVisitorImpl implements GenerateVisitor {

        private final TemplateText node;
        private final GeneratorVisitTracerFactory factory;
        private final Transaction tx;

        public GenerateVisitorImpl(Transaction tx, GeneratorVisitTracerFactory factory, TemplateText node) {
            this.tx = tx;
            this.factory = factory;
            this.node = node;
        }

        @Override
        public void writeScriptlet(String text) throws Exception {
            node.setText(text);
        }

        @Override
        public void visit(TemplateText text) throws Exception {
            factory.processText(tx, this, new TemplateTextImpl(text.getText()));
        }

    }

}
