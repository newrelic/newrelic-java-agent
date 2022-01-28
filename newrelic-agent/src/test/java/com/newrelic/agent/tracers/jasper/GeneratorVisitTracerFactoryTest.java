/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

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

import java.util.HashMap;
import java.util.Map;

public class GeneratorVisitTracerFactoryTest {

    private static final String APP_NAME = "Unit Test";

    public static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("apdex_t", 0.5f);
        return map;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        createServiceManager(createConfigMap());
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

    @Test
    public void comments1() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name=\"google-site-verification\" content=\"random-data\" />\n\t");
        sb.append("<meta name=\"msvalidate.01\" content=\"more-random-data\" />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"another-key\" />\n\t");
        sb.append("-->\n\n\t\t");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction playVideo()\n  {\n      }\n    </script>\n\n\t");
        sb.append("<anotherTag>OPT&reg;</anotherTag>\n\n\n</head>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void comments2() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name=\"google-site-verification\" content=\"random-data\" />\n\t");
        sb.append("<meta name=\"msvalidate.01\" content=\"more-random-data\" />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"another-key\" />\n\t");
        sb.append("-->\n\n\t\t");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction playVideo()\n  {\n      }\n    </script>\n\n\t<title>OPT&reg;</title>\n\n\n");
        sb.append("</head>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void gtInQuotes1() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name=\"google-site-verification\" content=\"random-data\" />\n\t");
        sb.append("<meta name=\"ms<validate>.01\" content=\"more-random-data\" />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"another-key\" />\n\t");
        sb.append("-->\n\n\t\t");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction playVideo()\n  {\n      }\n    </script>\n\n\t");
        sb.append("<anotherTag>OPT&reg;</title>\n\n\n</anotherTag>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void gtInQuotes2() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name=\"google-site-verification\" content=\"complex-data\" />\n\t");
        sb.append("<meta name=\"ms<validate>.01\" content=\"some-key\" />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"another-key\" />\n\t");
        sb.append("-->\n\n\t\t");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction newRelic()\n  {\n      }\n    </script>\n\n\t<title>OPT&reg;</title>\n\n\n");
        sb.append("</head>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void gtInQuotesMismatch() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name=\"google-s\"ite<-verif>ication\" content=\"keyverify\" />\n\t");
        sb.append("<meta name=\"ms<validate>.01\" content=\"randomdata\" />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"alsorandom\" />\n\t");
        sb.append("-->\n\n\t\t");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction newRelic()\n  {\n      }\n    </script>\n\n\t<title>OPT&reg;</title>\n\n\n");
        sb.append("</head>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void gtInSingleQuotes1() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name='google-site-verification' content='random-key\' />\n\t");
        sb.append("<meta name='ms<validate>.01' content='validation\' />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"anotherkey\" />\n\t");
        sb.append("-->\n\n\t\t");
        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction playVideo()\n  {\n      }\n    </script>\n\n\t");
        sb.append("<anotherTag>OPT&reg;</title>\n\n\n</anotherTag>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void gtInSingleQuotesMisMatch() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<head>");
        sb.append("\n\n\t");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\t");
        sb.append("<meta name='google-site-verification' content='some-glerp\' />\n\t");
        sb.append("<meta name='m's<validate>.01' content='abc123\' />\n\t");
        sb.append("<!-- <meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n\t");
        sb.append("-->\n\t");
        sb.append("<!--\n\t");
        sb.append("this is a comment\n\t");
        sb.append("<META name=\"y_key\" content=\"ourkey\" />\n\t");
        sb.append("-->\n\n\t\t");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<script language=\"javascript\">\n\tfunction doAThing()\n  {\n      }\n    </script>\n\n\t");
        sb.append("<anotherTag>OPT&reg;</title>\n\n\n</anotherTag>\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(beforeFooter + afterFooter);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void script1() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
        sb.append("<head>\n");
        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<anotherTag>Castor");
        sb.append("</anotherTag>\n\t");
        sb.append("</head>\n");
        sb.append("<script language=\"javascript\" >\n\t");
        sb.append("var strFrameInfoWrapper = \"\";\n");
        sb.append("strFrameInfoWrapper+= \"<body style='overflow-y:hidden;padding: 0px 0px 0px 0px; margin: 0px 0px 0px 0px;' >\";\n");
        sb.append("strFrameInfoWrapper+= \"<div id='hello-button' style='display:none; position: absolute; z-index: 9999; right:0px; top:-2px; cursor: hand;'>\";\n");
        sb.append("strFrameInfoWrapper+= \"</div>\";\n");
        sb.append("strFrameInfoWrapper+= \"</body>\";\n");
        sb.append("</script>\n\t");
        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</html>");
        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader + beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void script2() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("<title>New Relic");
        sb.append("</title>\n\t");
        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("</head>\n");
        sb.append("<script language=\"javascript\" >\n\t");
        sb.append("var strFrameInfoWrapper = \"\";\n");
        sb.append("strFrameInfoWrapper+= \"<body style='overflow-y:hidden;padding: 0px 0px 0px 0px; margin: 0px 0px 0px 0px;' >\";\n");
        sb.append("strFrameInfoWrapper+= \"<div id='communicationsButton' style='display:none; position: absolute; z-index: 9999; right:0px; top:-2px; cursor: hand;'>\";\n");
        sb.append("strFrameInfoWrapper+= \"</div>\";\n");
        sb.append("strFrameInfoWrapper+= \"</body>\";\n");
        sb.append("</script>\n\t");
        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</html>");
        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader + beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void noHeader() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<body>\n\t\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeFooter + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void noStartHeader() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n\t");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n\n");
        sb.append("<jsp:include page=\"start_header.jsp\" />\n");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("</head>\n");
        sb.append("<body>\n\t\n\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n\n</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader + beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void noBody() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\">\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta http-equiv=\"Expires\" content=\"Tue, 01 Jan 1980 1:00:00 GMT\"/>\n");
        sb.append("<meta http-equiv=\"Pragma\" content=\"no-cache\"/>\n");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n");
        sb.append("<meta name=\"Author\" content=\"my author\"/>\n");
        sb.append("<meta name=\"INFO\" content=\"mailto:support@newrelic.com\"/>\n");
        sb.append("<meta name=\"description\" content=\"We have a lot of fun in what we do.\"/>\n");
        sb.append("<meta name=\"KEYWORDS\" content=\"observability, new, relic\"/>\n");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<LINK href=\"/newrelic.png\" rel=\"shortcut icon\">\n");
        sb.append("<title>New Relic's neat service</title>\n");
        sb.append("<script type=\"text/javascript\">\n</script>\n</head>\n");
        sb.append("<frameset onload=\"try{window.frames['menu'].onmainloaded()}catch(err){}\" cols=\"172,*\" frameborder=\"no\" framespacing=\"0\">\n");
        sb.append("<frame src=\"Menu.jsp?action=loginpage\" name=\"menu\" noresize frameborder=\"1\" marginwidth=\"10\" marginheight=\"10\"/>\n");
        sb.append("<frame name=\"main\" noresize frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\"/> \n");
        sb.append("</frameset>\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeHeader + beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void multipleJsps1() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "index.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<jsp:include page=\"header.jsp\" />\n");
        sb.append("<% System.out.println(\"This is Roger's JSP\"); %>\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n");
        sb.append("</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeFooter + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);

        GeneratorVisitTracerFactory.noticeJspCompile(tx, "header.jsp");

        sb = new StringBuilder();
        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("<anotherTag>This is the Heading</anotherTag>\n");
        sb.append("</head>\n");
        sb.append("<body>");

        String afterHeader = sb.toString();

        node1 = new TemplateTextImpl(beforeHeader + afterHeader);
        visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + afterHeader;
        actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void multipleJsps2() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "index.jsp");

        StringBuilder sb = new StringBuilder();
        sb.append("<jsp:include page=\"header.jsp\" />\n");
        sb.append("<% System.out.println(\"This is Roger's JSP\"); %>\n");

        String beforeFooter = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n");
        sb.append("</html>");

        String afterFooter = sb.toString();

        TemplateTextImpl node1 = new TemplateTextImpl(beforeFooter + afterFooter);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        String expected = beforeFooter + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString();
        Assert.assertEquals(expected, actual);

        GeneratorVisitTracerFactory.noticeJspCompile(tx, "header.jsp");

        sb = new StringBuilder();
        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\" />\n");
        sb.append("<title>This is the Heading</title>\n");

        String beforeHeader = sb.toString();

        sb = new StringBuilder();
        sb.append("</head>\n");
        sb.append("<body>");

        String afterHeader = sb.toString();

        node1 = new TemplateTextImpl(beforeHeader + afterHeader);
        visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + afterHeader;
        actual = node1.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void incompleteNonMetaTags() throws Exception {
        GeneratorVisitTracerFactory factory = new GeneratorVisitTracerFactory();
        Transaction tx = Transaction.getTransaction();
        GeneratorVisitTracerFactory.noticeJspCompile(tx, "test.jsp");
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n  ");
        sb.append("<head>\n  ");
        String text1 = sb.toString();

        sb = new StringBuilder();
        sb.append("<link href=\"");
        String text2 = sb.toString();

        String beforeHeader = text1;

        sb = new StringBuilder();
        sb.append(" />\n    <link href=");
        String text3 = sb.toString();

        sb = new StringBuilder();
        sb.append(" />\n  </head>\n  <body>\n  ");
        String text4 = sb.toString();

        sb = new StringBuilder();
        sb.append("</body>\n</html>\n");
        String text5 = sb.toString();

        String beforeFooter = text2 + text3 + text4;

        String afterFooter = text5;

        TemplateTextImpl node1 = new TemplateTextImpl(text1 + text2);
        GenerateVisitorImpl visitor = new GenerateVisitorImpl(tx, factory, node1);
        factory.processText(tx, visitor, node1);

        TemplateTextImpl node2 = new TemplateTextImpl(text3);
        visitor = new GenerateVisitorImpl(tx, factory, node2);
        factory.processText(tx, visitor, node2);

        TemplateTextImpl node3 = new TemplateTextImpl(text4 + text5);
        visitor = new GenerateVisitorImpl(tx, factory, node3);
        factory.processText(tx, visitor, node3);

        String expected = beforeHeader + AbstractRUMState.BROWSER_TIMING_HEADER_CODE_SNIPPET + beforeFooter
                + AbstractRUMState.BROWSER_TIMING_FOOTER_CODE_SNIPPET + afterFooter;
        String actual = node1.toString() + node2.toString() + node3.toString();
        Assert.assertEquals(expected, actual);
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
