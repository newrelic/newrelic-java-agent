/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.agent.util.MockFilterConfig;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockServletConfig;
import org.apache.struts.mock.MockServletContext;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.mockito.Mockito;
import test.com.newrelic.agent.ServletDispatcher;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

import static org.mockito.Mockito.anyString;

public abstract class AgentHelper {

    private AgentHelper() {
    }

    public static void initializeConfig() {
        if (System.getProperty("newrelic.config.file") == null) {
            String config = getFullPath("/com/newrelic/agent/config/newrelic.yml");
            System.setProperty("newrelic.config.file", config);
        }
        AgentLogManager.addConsoleHandler();
        AgentLogManager.setLogLevel(Level.FINER.getName());
    }

    public static AgentConfig mockAgentConfig() {
        ServiceManager serviceManger = Mockito.spy(ServiceFactory.getServiceManager());
        ConfigService configService = Mockito.spy(serviceManger.getConfigService());

        Mockito.doReturn(configService).when(serviceManger).getConfigService();
        ServiceFactory.setServiceManager(serviceManger);

        AgentConfig agentConfig = Mockito.spy(configService.getDefaultAgentConfig());
        Mockito.doReturn(agentConfig).when(configService).getDefaultAgentConfig();
        Mockito.doReturn(agentConfig).when(configService).getAgentConfig(anyString());

        return agentConfig;
    }

    public static AgentConfig mockAgentConfig(TransactionTracerConfig transactionTracerConfig) {
        ServiceManager serviceManger = Mockito.spy(ServiceFactory.getServiceManager());
        ConfigService configService = Mockito.spy(serviceManger.getConfigService());
        Mockito.doReturn(configService).when(serviceManger).getConfigService();
        ServiceFactory.setServiceManager(serviceManger);

        AgentConfig agentConfig = Mockito.spy(configService.getDefaultAgentConfig());

        Mockito.doReturn(agentConfig).when(configService).getDefaultAgentConfig();
        Mockito.doReturn(agentConfig).when(configService).getAgentConfig(anyString());
        Mockito.doReturn(transactionTracerConfig).when(configService).getTransactionTracerConfig(anyString());

        return agentConfig;
    }

    public static MockServiceManager bootstrap(AgentConfig agentConfig) {
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, new HashMap<String, Object>());
        return new MockServiceManager(configService);
    }

    public static AgentConfig createAgentConfig(boolean collectTraces) {
        return createAgentConfig(collectTraces, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap());
    }

    public static AgentConfig createAgentConfig(boolean collectTraces, Map<String, Object> settings, Map<String, Object> serverData) {
        settings = new HashMap<>(settings);
        settings.put(AgentConfigImpl.APP_NAME, "Unit Test");
        serverData = new HashMap<>(serverData);
        serverData.put("collect_traces", collectTraces);
        AgentConfigFactory.mergeServerData(settings, serverData, null);
        return AgentConfigImpl.createAgentConfig(settings);
    }

    /**
     * Gets the full path to the file.
     * 
     * @param path Path starting at test.
     * @return The full path to the file.
     */
    public static File getFile(String path) {
        String fullPath = getFullPath(path);
        File file = new File(fullPath);

        if (!file.exists()) {
            Assert.fail(path + " was expanded to " + fullPath + " which does not exist.");
        }

        return file;
    }

    public static List<File> getFiles(String directory) {
        String fullPath = getFullPath(directory);
        File dirr = new File(fullPath);
        if (!dirr.isDirectory()) {
            Assert.fail("The input path should be a directory: " + directory);
        }

        File[] listFiles = dirr.listFiles();
        List<File> files = new ArrayList<>();
        for (File current : listFiles) {
            if (current.isFile()) {
                files.add(current);
            }
        }
        return files;
    }

    public static String getFullPath(String partialPath) {
        if (!partialPath.startsWith("/")) {
            partialPath = '/' + partialPath;
        }
        URL resource = AgentHelper.class.getResource(partialPath);

        if (resource == null) {
            return partialPath;
        }
        try {
            String path = URLDecoder.decode(resource.getPath(), "UTF-8");
            if (new File(path).exists()) {
                return path;
            }
        } catch (Exception ex) {
        }
        return resource.getPath();
    }

    public static Transaction invokeServlet(Servlet servlet, String contextPath, String applicationName,
            final String path) throws ServletException, IOException {
        Map<String, String> parameters = Collections.emptyMap();
        return invokeServlet(servlet, contextPath, applicationName, path, parameters);
    }

    public static MockServletContext createServletContext(final String servletContextName) {
        return new MockServletContext() {

            @Override
            public String getServletContextName() {
                return servletContextName;
            }

        };
    }

    public static Transaction invokeServlet(final Servlet servlet, String contextPath, final String applicationName,
            final String path, Map<String, String> parameters) throws ServletException, IOException {
        ServletContext context = null;
        if (servlet != null && servlet.getServletConfig() != null) {
            context = servlet.getServletConfig().getServletContext();
        }
        if (context == null) {
            context = createServletContext(applicationName);
        }
        return invokeServlet(servlet, contextPath, applicationName, path, parameters, context);
    }

    public static Transaction invokeServlet(final Servlet servlet, String contextPath, final String applicationName,
            final String path, Map<String, String> parameters, ServletContext context) throws ServletException,
            IOException {
        String servletPath = new StringTokenizer(path, "?").nextToken();
        FakeRequest request = new FakeRequest(contextPath, applicationName, servletPath, "", "", path);
        for (Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        FakeResponse response = new FakeResponse();
        MockServletConfig config = new MockServletConfig(context) {
            @Override
            public String getServletName() {
                return servlet == null ? null : servlet.getClass().getSimpleName();
            }
        };

        if (servlet != null) {
            ServletConfig existingConfig = servlet.getServletConfig();
            if (existingConfig != null) {
                Enumeration<String> parameterNames = existingConfig.getInitParameterNames();
                while (parameterNames.hasMoreElements()) {
                    String name = parameterNames.nextElement();
                    config.addInitParameter(name, existingConfig.getInitParameter(name));
                }
            }

            servlet.init(config);
        }

        Transaction transaction = Transaction.getTransaction();
        // transaction.setDispatcher(getWebRequestDispatcher(request, response, transaction));

        if (transaction.isStarted()) {
            servlet.service(request, response);
        } else {

            ServletDispatcher.registerTracerFactory();
            ServletDispatcher.getServletDispatcher(request, response);
            ServletDispatcher.dispatch(servlet, request, response);
        }
        return transaction;
    }

    public static Dispatcher getWebRequestDispatcher(final MockHttpServletRequest request,
            MockHttpServletResponse response, Transaction transaction) {
        Request req = new Request() {

            @Override
            public String getRequestURI() {
                return request.getRequestURI();
            }

            @Override
            public String getRemoteUser() {
                return request.getRemoteUser();
            }

            @Override
            public String[] getParameterValues(String name) {
                return request.getParameterValues(name);
            }

            @Override
            public Enumeration getParameterNames() {
                return request.getParameterNames();
            }

            @Override
            public String getHeader(String name) {
                return request.getHeader(name);
            }

            @Override
            public String getCookieValue(String name) {
                return null;
            }

            @Override
            public Object getAttribute(String name) {
                return request.getAttribute(name);
            }

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }
        };
        Response res = new Response() {

            @Override
            public int getStatus() throws Exception {
                return 0;
            }

            @Override
            public String getStatusMessage() throws Exception {
                return null;
            }

            @Override
            public void setHeader(String name, String value) {
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

        };
        return new com.newrelic.agent.dispatchers.WebRequestDispatcher(req, res, transaction);
    }

    public static void verifyDatastoreMetrics(Set<String> actualMetrics, DatastoreVendor vendor, String table,
            String operation) {
        String operationMetric = MessageFormat.format(DatastoreMetrics.OPERATION_METRIC, vendor, operation);
        String statementMetric = MessageFormat.format(DatastoreMetrics.STATEMENT_METRIC, vendor, table, operation);
        verifyMetrics(actualMetrics, "Datastore/all", "Datastore/" + vendor + "/all", operationMetric);
        boolean allWeb = actualMetrics.contains("Datastore/allWeb");
        if (allWeb) {
            Assert.assertTrue(actualMetrics.contains("Datastore/allWeb"));
            Assert.assertTrue(actualMetrics.contains("Datastore/" + vendor + "/allWeb"));
        } else {
            Assert.assertTrue(actualMetrics.contains("Datastore/allOther"));
            Assert.assertTrue(actualMetrics.contains("Datastore/" + vendor + "/allOther"));
        }

        if (null != table) {
            verifyMetrics(actualMetrics, statementMetric);
        }
    }

    public static void verifyMetrics(String... expectedMetricNames) {
        Set<String> metrics = AgentHelper.getMetrics(getDefaultStatsEngine());
        verifyMetrics(metrics, expectedMetricNames);
    }

    public static void verifyMetrics(Set<String> actualMetrics, String... expectedMetricNames) {
        Set<String> missingMetrics = new HashSet<>(Arrays.asList(expectedMetricNames));
        missingMetrics.removeAll(actualMetrics);
        Assert.assertTrue(MessageFormat.format("Missing metrics: [{0}] all metrics: [{1}] (java.version={2})",
                missingMetrics, actualMetrics, System.getProperty("java.version")), missingMetrics.isEmpty());
    }

    public static Set<String> getMetrics() {
        return getMetrics(true);
    }

    public static void clearMetrics() {
        getDefaultStatsEngine().clear();
    }

    public static List<Tracer> getChildren(Tracer parent) {

        /*
         * if (children == null) { return Collections.emptyList(); } return children;
         */
        if (parent.isParent()) {
            List<Tracer> kids = new ArrayList<>();
            for (Tracer tracer : ((AbstractTracer) parent).getTransactionActivity().getTracers()) {
                if (parent == tracer.getParentTracer()) {
                    kids.add(tracer);
                }
            }
            return kids;
        } else {
            return Collections.emptyList();
        }
    }

    public static Collection<Tracer> getTracers(Tracer rootTracer) {
        Collection<Tracer> result = new ArrayList<>();
        List<Tracer> pending = new ArrayList<>();
        pending.add(rootTracer);
        while (!pending.isEmpty()) {
            Tracer tracer = pending.remove(0);
            result.add(tracer);
            pending.addAll(getChildren(tracer));
        }
        return result;
    }

    /**
     * Consider using {@link TransactionDataList} instead of this.
     */
    public static Set<String> getMetrics(boolean includeScoped) {
        return getMetrics(getDefaultStatsEngine(), includeScoped);
    }

    /**
     * Consider using {@link TransactionDataList} instead of this.
     */
    public static Set<String> getMetrics(StatsEngine statsEngine, boolean includeScoped) {
        Set<String> result = new HashSet<>();
        List<MetricName> metricNames = statsEngine.getMetricNames();
        for (MetricName metricName : metricNames) {
            if (includeScoped || !metricName.isScoped()) {
                result.add(metricName.getName());
            }
        }
        return result;
    }

    public static Set<String> getMetrics(StatsEngine statsEngine) {
        return getMetrics(statsEngine, true);
    }

    public static Set<String> getMetrics(Set<MetricName> metricNames, boolean includeScoped) {
        Set<String> metrics = new HashSet<>();
        for (MetricName metricName : metricNames) {
            if (includeScoped || !metricName.isScoped()) {
                metrics.add(metricName.getName());
            }
        }
        return metrics;
    }

    public static Set<MetricName> getMetricNames(Collection<MetricData> dataList) {
        Set<MetricName> metrics = new HashSet<>();
        for (MetricData data : dataList) {
            MetricName metricName = data.getMetricName();
            metrics.add(metricName);
        }
        return metrics;
    }

    public static List<String> getScopes(Collection<MetricName> metricNames, String metricName) {
        List<String> scopes = new ArrayList<>();
        for (MetricName name : metricNames) {
            if (name.getName().equals(metricName)) {
                scopes.add(name.getScope());
            }
        }
        return scopes;
    }

    public static IRPMService getDefaultRPMService() {
        return ServiceFactory.getRPMService();
    }

    public static StatsEngine getDefaultStatsEngine() {
        StatsService statsService = ServiceFactory.getStatsService();
        return statsService.getStatsEngineForHarvest(null);
    }

    public static StatsEngine createTestStatsEngine() {
        return new StatsEngineImpl();
    }

    public static Object serializeJSON(Object obj) throws Exception {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(oStream);
        JSONValue.writeJSONString(obj, writer);
        writer.close();
        String json = oStream.toString();
        JSONParser parser = new JSONParser();
        return parser.parse(json);
    }

    public static Object serializeJSONusingDataSenderWriter(Object obj) throws Exception {
        String json = DataSenderWriter.toJSONString(obj);
        JSONParser parser = new JSONParser();
        return parser.parse(json);
    }

    public static byte[] serializeJSONToByteArray(Object obj) throws Exception {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(oStream);
        JSONValue.writeJSONString(obj, writer);
        writer.close();
        return oStream.toByteArray();
    }

    public static Filter initializeFilter(Filter filter, ServletContext servletContext, Map<String, String> config)
            throws ServletException {
        MockFilterConfig filterConfig = new MockFilterConfig();
        if (servletContext == null) {
            servletContext = new MockServletContext();
        }
        filterConfig.setServletContext(servletContext);
        for (Entry<String, String> entry : config.entrySet()) {
            filterConfig.setInitParameter(entry.getKey(), entry.getValue());
        }

        filter.init(filterConfig);

        return filter;
    }

    public static Servlet initializeServlet(Servlet servlet, ServletContext servletContext, Map<String, String> config)
            throws ServletException {
        MockServletConfig c = new MockServletConfig();
        if (servletContext == null) {
            servletContext = new MockServletContext();
        }
        c.setServletContext(servletContext);
        for (Entry<String, String> entry : config.entrySet()) {
            c.addInitParameter(entry.getKey(), entry.getValue());
        }
        servlet.init(c);
        return servlet;
    }

    public static void setLastTracer(Tracer tr) {
        // There are a bunch of unit tests that new() some Tracer and then call
        // finishTracer() without calling startTracer(). This causes "Inconsistent
        // state! ..." errors since the tracer stack doesn't balance. These used
        // to be harmless, but starting with the async transaction refactor in
        // August 2014 the error recovery code screws up the life cycle of some
        // object(s). So now we jam the fake Tracer into lastTracer() using
        // reflection to prevent the error.

        Field field;
        try {
            field = TransactionActivity.class.getDeclaredField("lastTracer");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        field.setAccessible(true);

        try {
            field.set(TransactionActivity.get(), tr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
