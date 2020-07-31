/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.FakeRequest;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Trace;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockServletConfig;
import org.apache.struts.mock.MockServletContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import test.newrelic.test.agent.TraceAnnotationTest.NewRelicIgnoreTransaction;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class AgentTest {

    private static final String APPLICATION_NAME = "com.newrelic.agent.APPLICATION_NAME";
    private static final String TRANSACTION_NAME = "com.newrelic.agent.TRANSACTION_NAME";
    public static final String APPLICATION_NAME_2 = "BenderUnitTest2";
    private ServiceManager serviceManager;

    @Before
    public void before() {
        serviceManager = ServiceFactory.getServiceManager();
        Transaction.clearTransaction();
    }

    @After
    public void after() {
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void servletInit() throws Exception {
        TestServlet servlet = new TestServlet();
        ServletContext context = new MockServletContext();
        MockServletConfig config = new MockServletConfig(context);
        servlet.init(config);

        Set<String> metrics = AgentHelper.getMetrics();

        AgentHelper.verifyMetrics(metrics, MetricNames.OTHER_TRANSACTION_ALL);
    }

    @Test
    public void buildDate() {
        String buildDate = AgentJarHelper.getBuildDate();
        Assert.assertNotNull(buildDate);
    }

    @Test
    public void testServerInfo() throws ServletException, IOException {
        TestServlet servlet = new TestServlet();
        String path = "/my/word";
        AgentHelper.invokeServlet(servlet, "", "Test", path);

        Assert.assertEquals("MockServletContext",
                ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity().getDispatcher());
        Assert.assertEquals("$Version$",
                ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity().getDispatcherVersion());
    }

    @Test
    public void testAnnotations() throws SecurityException, NoSuchMethodException {
        InstrumentedClass annotation = HttpServlet.class.getAnnotation(InstrumentedClass.class);
        Assert.assertNotNull(annotation);
        Assert.assertFalse(annotation.legacy());

        Method m = HttpServlet.class.getMethod("service", ServletRequest.class, ServletResponse.class);
        Assert.assertNotNull(m);
        InstrumentedMethod methodAnnotation = m.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(methodAnnotation);

        Assert.assertEquals(InstrumentationType.TracedWeaveInstrumentation, methodAnnotation.instrumentationTypes()[0]);
        // Assert.assertEquals(HttpServletPointCut.class.getName(), methodAnnotation.instrumentationName());

        // m = HttpServlet.class.getMethod("service", HttpServletRequest.class, HttpServletResponse.class);
        // annotations = m.getAnnotations();
        // Assert.assertTrue(annotations.length == 0);
        // Assert.assertTrue(Arrays.asList(annotations).toString(), annotations[0] instanceof Instrumented);
    }

    @Test
    public void transactionTraceDisabled() throws Exception {
        try {
            TransactionDataList txList = new TransactionDataList();
            ServiceFactory.getTransactionService().addTransactionListener(txList);

            AgentHelper.invokeServlet(new TestServlet(), "", "App", "/foo/bar");

            synchronized (this) {
                wait(1000);
            }

            Assert.assertEquals(2, txList.size());

            TransactionData transactionData = txList.get(1);
            Tracer dispatcherTracer = transactionData.getRootTracer();
            Assert.assertTrue(dispatcherTracer.getDuration() > dispatcherTracer.getExclusiveDuration());
        } finally {
        }
    }

    /*
     * @Test public void filterOnlyRequest() throws ServletException, Exception { final String applicationName =
     * APPLICATION_NAME_2;
     * 
     * TestServlet servlet = new TestServlet(); String path = "/my/word"; Transaction tx = invokeFilter(null, new
     * Filter() {
     * 
     * @Override public void destroy() {}
     * 
     * @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws
     * IOException, ServletException { filterChain.doFilter(request, response); }
     * 
     * @Override public void init(FilterConfig arg0) throws ServletException {}
     * 
     * }, "", applicationName, path);
     * 
     * Set<String> metrics = AgentHelper.getMetrics();
     * 
     * Assert.assertTrue(metrics.toString(), metrics.contains("Servlet/" + TestServlet.class.getName() + "/service"));
     * 
     * PriorityTransactionName transactionName = tx.getPriorityTransactionName();
     *
     * Assert.assertEquals("WebTransaction/Servlet/TestServlet", transactionName.getName()); }
     */

    @Test
    public void filterConfigOverridingTxName() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        String path = "/my/word";
        Transaction tx = invokeFilter(null, AgentHelper.initializeFilter(new DummyFilter(), null, ImmutableMap.of(
                TRANSACTION_NAME, "/dude")), "", applicationName, path);

        Assert.assertEquals("WebTransaction/CustomFilter/dude", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void filterNameAppWithServletContextName() throws ServletException, Exception {
        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        final DummyFilter filter = new DummyFilter();
        FilterConfig config = createFilterConfig();
        Mockito.when(config.getServletContext().getServletContextName()).thenReturn("ContextName!");
        filter.init(config);

        Transaction tx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() throws Exception {

                filter.doFilter(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class),
                        Mockito.mock(FilterChain.class));

                return Transaction.getTransaction();
            }
        }.call();

        Assert.assertEquals("ContextName!", tx.getPriorityApplicationName().getName());
    }

    private FilterConfig createFilterConfig() {
        FilterConfig config = Mockito.mock(FilterConfig.class);

        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(config.getServletContext()).thenReturn(context);

        return config;
    }

    private ServletConfig createServletConfig() {
        ServletConfig config = Mockito.mock(ServletConfig.class);

        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(config.getServletContext()).thenReturn(context);

        return config;
    }

    @Test
    public void filterNameAppWithServletContextPath() throws ServletException, Exception {
        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        final DummyFilter filter = new DummyFilter();
        FilterConfig config = createFilterConfig();
        Mockito.when(config.getServletContext().getContextPath()).thenReturn("/myapp");
        filter.init(config);

        Transaction tx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() throws Exception {

                filter.doFilter(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class),
                        Mockito.mock(FilterChain.class));

                return Transaction.getTransaction();
            }
        }.call();

        Assert.assertEquals("myapp", tx.getPriorityApplicationName().getName());
    }

    @Test
    public void testMultipleRequestInitializedAndDestroyedCalls() {
        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Assert.assertNotNull(agentConfig);

        final com.newrelic.api.agent.Request request = Mockito.mock(com.newrelic.api.agent.Request.class);
        Mockito.doReturn(HeaderType.HTTP).when(request).getHeaderType();

        // final HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        final com.newrelic.api.agent.Response response = Mockito.mock(com.newrelic.api.agent.Response.class);

        // Assert that the dispatcher starts null, gets initialized, and then doesn't change.

        Transaction tx = Transaction.getTransaction();
        Assert.assertNull(tx.getDispatcher());
        tx.requestInitialized(request, response);
        Dispatcher disp = tx.getDispatcher();
        Assert.assertNotNull(disp);
        tx.requestInitialized(request, response);
        Assert.assertEquals(disp, tx.getDispatcher());

        tx.requestDestroyed();
        // There's no obvious way to tell that the transaction complete, so the test just
        // checks that we don't throw an exception here:
        tx.requestDestroyed();
    }

    @Test
    public void testSupportabilityMetrics() {
        String requestInitializedSupportabilityMetric = "Supportability/Transaction/RequestInitialized";
        String requestDestroyedSupportabilityMetric = "Supportability/Transaction/RequestDestroyed";
        String classloaderTimeSupportabilityMetric = "Supportability/Classloader/TransformTime";

        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Assert.assertNotNull(agentConfig);

        // final HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        final com.newrelic.api.agent.Request request = Mockito.mock(com.newrelic.api.agent.Request.class);
        Mockito.when(request.getHeaderType()).thenReturn(HeaderType.HTTP);

        // final HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        final com.newrelic.api.agent.Response response = Mockito.mock(com.newrelic.api.agent.Response.class);

        // Assert that the dispatcher starts null, gets initialized, and then doesn't change.

        Transaction tx = Transaction.getTransaction();
        Assert.assertNull(tx.getDispatcher());
        tx.requestInitialized(request, response);
        Dispatcher disp = tx.getDispatcher();
        Assert.assertNotNull(disp);
        tx.requestInitialized(request, response);
        Assert.assertEquals(disp, tx.getDispatcher());

        tx.requestDestroyed();
        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("requestInitialized Supportability Metric",
                metricData.get(requestInitializedSupportabilityMetric));
        Assert.assertNotNull("requestDestroyed Supportability Metric",
                metricData.get(requestDestroyedSupportabilityMetric));
        Assert.assertNotNull("classloaderTime Supportability Metric",
                metricData.get(classloaderTimeSupportabilityMetric));

        // There's no obvious way to tell that the transaction complete, so the test just
        // checks that we don't throw an exception here:
        tx.requestDestroyed();
    }

    /**
     * Mocks generated by Mockito are CGLib generated. As a result, they trigger our proxy detector which prevents us
     * from instrumenting them. As a result, we cannot use Mockito to mock any interfaces we instrument - in this case,
     * the Servlet interface. We need to manually maintain our own mock.
     */
    private static class MockServlet implements Servlet {

        private final ServletConfig config;

        public MockServlet(ServletConfig config) {
            this.config = config;
        }

        @Override
        public void init(ServletConfig config) throws ServletException {
        }

        @Override
        public ServletConfig getServletConfig() {
            return this.config;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {
        }

    }

    @Test
    public void servletNameAppWithServletContextPath() throws ServletException, Exception {
        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        ServletConfig config = createServletConfig();
        Mockito.when(config.getServletContext().getContextPath()).thenReturn("/myapp");
        final Servlet servlet = new MockServlet(config);

        Transaction tx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() throws Exception {

                servlet.service(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));

                return Transaction.getTransaction();
            }
        }.call();

        Assert.assertEquals("myapp", tx.getPriorityApplicationName().getName());
    }

    @Test
    public void servletStartTime() throws ServletException, Exception {
        ServletConfig config = createServletConfig();
        final Servlet servlet = new MockServlet(config);

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        long queueTime = System.currentTimeMillis() - 5;
        Mockito.when(request.getHeader("X-Queue-Start")).thenReturn("t=" + queueTime);
        long requestTime = System.currentTimeMillis() - 100;
        Mockito.when(request.getHeader("X-Request-Start")).thenReturn("t=" + requestTime);

        Transaction tx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() throws Exception {

                servlet.service(request, Mockito.mock(HttpServletResponse.class));

                return Transaction.getTransaction();
            }
        }.call();

        long externalTime = ((WebRequestDispatcher) tx.getDispatcher()).getQueueTime();
        long txStartTime = tx.getWallClockStartTimeMs();
        long expectedQueueTime = txStartTime - queueTime;
        long expectedRequestTime = 0; // Since we no longer track "X-Request-Start" separately this will be 0
        Assert.assertEquals(expectedRequestTime + expectedQueueTime, externalTime, 1); // allow for rounding
    }

    /**
     * And this test shows an example of the problem described above - we cannot rely on instrumenting Mockito-generated
     * classes.
     *
     * @throws ServletException
     * @throws Exception
     */
    @Test
    public void noServletNameAppWithServletContextPath_IfMockito() throws ServletException, Exception {
        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        final Servlet servlet = Mockito.mock(Servlet.class);
        ServletConfig config = createServletConfig();
        Mockito.when(config.getServletContext().getContextPath()).thenReturn("/myapp");
        Mockito.when(servlet.getServletConfig()).thenReturn(config);

        Transaction tx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() throws Exception {

                servlet.service(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));

                return Transaction.getTransaction();
            }
        }.call();

        // Here's the "failure" - getName() is null because we didn't instrument the Mockito-generated servlet.
        String defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        Assert.assertEquals(defaultAppName, tx.getPriorityApplicationName().getName());
    }

    @Test
    public void filterConfigOverridingAppName() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        String path = "/my/word";
        Transaction tx = invokeFilter(null, AgentHelper.initializeFilter(new DummyFilter(), null, ImmutableMap.of(
                APPLICATION_NAME, "OtherName")), "", applicationName, path);

        Assert.assertEquals("OtherName", tx.getPriorityApplicationName().getName());

        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(false);

        tx = invokeFilter(null, AgentHelper.initializeFilter(new DummyFilter(), null, ImmutableMap.of(APPLICATION_NAME,
                "OtherName")), "", applicationName, path);

        String defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        // New transaction must have the default application name defined in the yml file
        // since auto application naming is disabled
        Assert.assertEquals(defaultAppName, tx.getPriorityApplicationName().getName());
    }

    @Test
    public void contextConfigOverridingFilterAppName() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        String path = "/my/word";
        MockServletContext context = AgentHelper.createServletContext("ContextName");
        context.addInitParameter(APPLICATION_NAME, "OverriddenName");
        Transaction tx = invokeFilter(null, AgentHelper.initializeFilter(new DummyFilter(), context,
                ImmutableMap.<String, String>of()), "", applicationName, path);

        Assert.assertEquals("OverriddenName", tx.getPriorityApplicationName().getName());

        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(false);

        context = AgentHelper.createServletContext("ContextName");
        context.addInitParameter(APPLICATION_NAME, "OverriddenName");
        tx = invokeFilter(null, AgentHelper.initializeFilter(new DummyFilter(), context,
                ImmutableMap.<String, String>of()), "", applicationName, path);

        String defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        // New transaction must have the default application name defined in the yml file
        // since auto application naming is disabled
        Assert.assertEquals(defaultAppName, tx.getPriorityApplicationName().getName());
    }

    @Test
    public void normalRequestWithFilter() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        TestServlet servlet = new TestServlet();
        String path = "/my/word";
        Transaction tx = invokeFilter(servlet, new DummyFilter(), "", applicationName, path);

        Set<String> metrics = AgentHelper.getMetrics();

        Assert.assertTrue(metrics.toString(), metrics.contains("Servlet/" + TestServlet.class.getName() + "/service"));

        PriorityTransactionName transactionName = tx.getPriorityTransactionName();

        Assert.assertEquals("WebTransaction/Servlet/TestServlet", transactionName.getName());
    }

    @Test
    public void servletConfigOverridingTxName() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        Servlet servlet = AgentHelper.initializeServlet(new TestServlet(), null, ImmutableMap.of(TRANSACTION_NAME,
                "/dude"));
        String path = "/my/word";
        Transaction tx = AgentHelper.invokeServlet(servlet, "", applicationName, path);

        Assert.assertEquals("WebTransaction/CustomServlet/dude", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void servletConfigOverridingAppName() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        Servlet servlet = AgentHelper.initializeServlet(new TestServlet(), null, ImmutableMap.of(APPLICATION_NAME,
                "OhDude!"));
        String path = "/my/word";
        Transaction tx = AgentHelper.invokeServlet(servlet, "", applicationName, path);

        Assert.assertEquals("OhDude!", tx.getPriorityApplicationName().getName());

        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(false);

        servlet = AgentHelper.initializeServlet(new TestServlet(), null, ImmutableMap.of(APPLICATION_NAME, "OhDude!"));
        tx = AgentHelper.invokeServlet(servlet, "", applicationName, path);

        String defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        // New transaction must have the default application name defined in the yml file
        // since auto application naming is disabled
        Assert.assertEquals(defaultAppName, tx.getPriorityApplicationName().getName());
    }

    //@Test
    public void contextConfigOverridingServletAppName() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        AgentConfig agentConfig = AgentHelper.mockAgentConfig();
        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(true);

        MockServletContext context = AgentHelper.createServletContext("ContextName");
        context.addInitParameter(APPLICATION_NAME, "OverriddenName");
        Servlet servlet = AgentHelper.initializeServlet(new TestServlet(), context, ImmutableMap.<String, String>of());
        String path = "/my/word";
        Transaction tx = AgentHelper.invokeServlet(servlet, "", applicationName, path);

        Assert.assertEquals("OverriddenName", tx.getPriorityApplicationName().getName());

        Mockito.when(agentConfig.isAutoAppNamingEnabled()).thenReturn(false);

        context = AgentHelper.createServletContext("ContextName");
        context.addInitParameter(APPLICATION_NAME, "OverriddenName");
        servlet = AgentHelper.initializeServlet(new TestServlet(), context, ImmutableMap.<String, String>of());
        tx = AgentHelper.invokeServlet(servlet, "", applicationName, path);

        String defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        // New transaction tx must have the default application name defined in the yml file
        // since auto application naming is disabled
        Assert.assertEquals(defaultAppName, tx.getPriorityApplicationName().getName());
    }

    @WebServlet(urlPatterns = { "one", "two" })
    private static class AnnotationTest extends GenericServlet {
        Transaction transaction;

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            this.transaction = Transaction.getTransaction();
        }
    }

    @Test
    public void webServletAnnotation() throws ServletException, Exception {

        AnnotationTest annotationTest = new AnnotationTest();
        annotationTest.service(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));

        Assert.assertEquals("WebTransaction/WebServletPath/one",
                annotationTest.transaction.getPriorityTransactionName().getName());
    }

    @Test
    public void normalRequest() throws ServletException, Exception {
        final String applicationName = APPLICATION_NAME_2;

        TestServlet servlet = new TestServlet();
        String path = "/my/word";
        Transaction tx = AgentHelper.invokeServlet(servlet, "", applicationName, path);

        // Assert.assertTrue(Agent.instance().getApplicationNames().toString(),
        // Agent.instance().getApplicationNames().contains(applicationName));
        Set<String> metrics = AgentHelper.getMetrics();

        Assert.assertTrue(metrics.toString(), metrics.contains("Servlet/" + TestServlet.class.getName() + "/service"));

        PriorityTransactionName transactionName = tx.getPriorityTransactionName();

        Assert.assertEquals("WebTransaction/Servlet/TestServlet", transactionName.getName());

        // / Assert.assertTrue(metrics.toString(), metrics.contains(MetricNames.DISPATCHER));
        // Assert.assertTrue(metrics.toString(), metrics.contains(MetricNames.FRONTEND + path));
        // Assert.assertTrue(metrics.toString(), metrics.contains("ServletInit/" + TestServlet.class.getSimpleName()));

        /*
         * boolean blameOk = false; for (MetricSpec spec : metricsSpecs) { if (spec.getName().equals("Servlet/" +
         * TestServlet.class.getSimpleName() + "/service") && spec.getScope().equals(MetricNames.FRONTEND + path))
         * blameOk = true; } Assert.assertTrue(blameOk);
         */
    }

    /**
     * If the transaction trace size limit is exceeded, a tracer should record metrics but not be part of the
     * transaction trace.
     */
    @Test
    public void ttSizeLimitExceeded() throws ServletException, IOException {
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);
        TestSizeLimitServlet servlet = new TestSizeLimitServlet();
        String path = "/my/word";
        AgentHelper.invokeServlet(servlet, "", APPLICATION_NAME_2, path);
        StatsEngine statsEngine = AgentHelper.getDefaultStatsEngine();
        MetricName metricName = MetricName.create(
                "Custom/test.newrelic.test.agent.AgentTest$TestSizeLimitServlet/doNothing",
                "WebTransaction/Servlet/TestSizeLimitServlet");
        ResponseTimeStats stats = statsEngine.getResponseTimeStats(metricName);
        Assert.assertEquals(2, stats.getCallCount());
        Assert.assertEquals(2, txs.size());
        TransactionData transactionData = txs.get(1);
        Collection<Tracer> tracers = AgentHelper.getTracers(transactionData.getRootTracer());
        Assert.assertEquals(3, tracers.size());
    }

    /**
     * Tests that a transaction is ignored when {@link Transaction#setIgnore(boolean)} is set to true.
     */
    @Test
    public void ignoreTransaction() throws Exception {
        Servlet servlet = new DummyServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                    IOException {
                go();
                Transaction.getTransaction().setIgnore(true);

                for (int i = 0; i < 10; i++) {
                    go();
                }
                Collection<Tracer> children = AgentHelper.getChildren(Transaction.getTransaction().getTransactionActivity().getLastTracer());
                Assert.assertEquals(1, children.size());
            }

            @Trace
            public void go() {

            }

        };

        for (int i = 0; i < 100; i++) {
            AgentHelper.invokeServlet(servlet, "ignore", "Test", "/foo/bar");
        }

        Set<String> metrics = AgentHelper.getMetrics(AgentHelper.getDefaultStatsEngine());
        Assert.assertFalse(metrics.contains("Servlet/" + servlet.getClass().getName() + "/service"));
    }

    @Test
    public void ignoreTransaction2() throws Exception {

        final List<Tracer> txData = new ArrayList<>();
        Servlet servlet = new DummyServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                    IOException {
                txData.add(Transaction.getTransaction().getTransactionActivity().getLastTracer());
                Dude dude = new Dude();
                dude.go().go();
                // specifically NOT referring to our constant to verify that it doesn't need to be the same object
                request.setAttribute("com.newrelic.agent.IGNORE", true);

                Assert.assertTrue(Transaction.getTransaction().isIgnore());

                for (int i = 0; i < 100; i++) {
                    dude.go();
                }
            }

        };
        AgentHelper.invokeServlet(servlet, "ignore", "Test", "/foo/bar");

        Assert.assertEquals(1, txData.size());
    }

    @Test
    public void ignoreTransaction3() throws Exception {

        final List<Tracer> txData = new ArrayList<>();
        Servlet servlet = new DummyServlet() {

            private static final long serialVersionUID = 1L;

            @NewRelicIgnoreTransaction
            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                    IOException {
                txData.add(Transaction.getTransaction().getTransactionActivity().getLastTracer());
                Dude dude = new Dude();
                dude.go().go();

                Assert.assertTrue(Transaction.getTransaction().isIgnore());

                for (int i = 0; i < 100; i++) {
                    dude.go();
                }
            }
        };
        AgentHelper.invokeServlet(servlet, "ignore", "Test", "/foo/bar");

        Assert.assertEquals(1, txData.size());
    }

    @Test
    public void ignoreApdex() throws Exception {

        final List<Dispatcher> txData = new ArrayList<>();
        Servlet servlet = new DummyServlet() {
            private static final long serialVersionUID = 1L;

            @NewRelicIgnoreApdex
            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                    IOException {
                Tracer lastTracer = Transaction.getTransaction().getTransactionActivity().getLastTracer();
                txData.add(Transaction.getTransaction().getDispatcher());
                Dude dude = new Dude();
                dude.go().go();

                Assert.assertEquals(2, AgentHelper.getChildren(lastTracer).size());
            }

        };
        AgentHelper.invokeServlet(servlet, "ignore", "Test", "/foo/bar");

        Assert.assertEquals(1, txData.size());
        WebRequestDispatcher dispatcher = (WebRequestDispatcher) txData.get(0);
        Assert.assertTrue(dispatcher.isIgnoreApdex());
    }

    @Test
    public void anonymousClass() throws Exception {
        Servlet servlet = new DummyServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                    IOException {
                synchronized (this) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        };

        AgentHelper.invokeServlet(servlet, "test", "Test", "/foo/bar");

        Set<String> metrics = AgentHelper.getMetrics(AgentHelper.getDefaultStatsEngine());
        AgentHelper.verifyMetrics(metrics, "Servlet/" + servlet.getClass().getName() + "/service");
    }

    @Trace(dispatcher = true)
    public static Transaction invokeFilter(final Servlet servlet, Filter filter, final String contextPath,
            final String applicationName, final String path) throws IOException, ServletException {
        final AtomicReference<Transaction> transaction = new AtomicReference<>();

        String servletPath = new StringTokenizer(path, "?").nextToken();
        final MockHttpServletRequest request = new FakeRequest(contextPath, applicationName, servletPath, "", "", path);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = new FilterChain() {

            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                Transaction tx = Transaction.getTransaction();
                transaction.set(tx);
                if (servlet != null) {
                    AgentHelper.invokeServlet(servlet, contextPath, applicationName, path);
                }
                tx.setDispatcher(AgentHelper.getWebRequestDispatcher(request, response, tx));
            }
        };

        filter.doFilter(request, response, chain);

        return transaction.get();
    }

    /*
     * @Test public void jsp() throws Exception { Servlet servlet = new DummyJsp(); for (int i = 0; i < 5; i++)
     * AgentHelper.invokeServlet(servlet, "test", "Test", "/foo/bar");
     * 
     * Set<String> metrics = AgentHelper.getMetrics(AgentHelper.getDefaultRPMService());
     * AgentHelper.verifyMetrics(metrics, "Servlet/" + servlet.getClass().getName() + "/service"); }
     */

    private static final class DummyFilter implements Filter {
        @Override
        public void destroy() {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
                throws IOException, ServletException {
            filterChain.doFilter(request, response);
        }

        @Override
        public void init(FilterConfig arg0) throws ServletException {
        }
    }

    public class DummyServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

    }

    public static class TestSizeLimitServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        public TestSizeLimitServlet() {
        }

        @Override
        public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {
            doNothing();
            int ttSizeLimit = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionSizeLimit();
            Transaction.getTransaction().getTransactionCounts().incrementSize(ttSizeLimit);
            doNothing();
        }

        @Trace
        private void doNothing() {
        }

    }

    public class TestServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        boolean error = false;
        private final int sleep;

        public TestServlet() {
            this(100);
        }

        public TestServlet(int sleep) {
            this.sleep = sleep;
        }

        @Override
        public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {
            synchronized (this) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (error) {
                throw new ServletException("Something bad happened");
            }
        }
    }

    public class TestFilter implements Filter {

        @Override
        public void destroy() {
        }

        @Override
        public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException,
                ServletException {
        }

        @Override
        public void init(FilterConfig arg0) throws ServletException {
        }

    }

    public class Dude {
        @Trace
        public Dude go() {
            return this;
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NewRelicIgnoreApdex {
    }

}
