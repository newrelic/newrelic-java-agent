/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.UnknownDatabaseVendor;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootSqlTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.NewRelicApiImplementation;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;
import sql.DummyConnection;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class RequestUriConfigTests {

    @Parameterized.Parameters(name = "{index}:{0}:{1}")
    public static Collection<Object[]> getParameters() throws Exception {
        JSONArray jsonTests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/uncross_agent_tests/request_uri_java_only.json");
        List<Object[]> testParameters = new LinkedList<>();
        for (Object jsonTest : jsonTests) {
            JSONObject testObj = (JSONObject) jsonTest;
            testParameters.add(new Object[] {
                    testObj.get("test_type"),
                    testObj.get("test_name"),
                    testObj
            });
        }

        return testParameters;
    }

    @Parameterized.Parameter(0)
    public String testType;

    @Parameterized.Parameter(1)
    public String testName;

    @Parameterized.Parameter(2)
    public JSONObject jsonTest;

    @Test
    public void test() throws Exception {
        switch(testType) {
            case "slow_sql":
                RequestUriConfigSqlTest datastoreTest = new RequestUriConfigSqlTest(jsonTest);
                runSqlTest(datastoreTest);
                break;
            case "transaction_trace":
                RequestUriConfigTransactionTest transactionTest = new RequestUriConfigTransactionTest(jsonTest);
                runTransactionTraceTest(transactionTest);
                break;
            case "traced_error":
                RequestUriConfigTransactionTest errorTest = new RequestUriConfigTransactionTest(jsonTest);
                runTracedErrorTest(errorTest);
                break;
            default:
                fail(MessageFormat.format("Unknown test type >>{0}<<; expected slow_sql or transaction_trace or traced_error", testType));
                break;
        }
    }

    public void runSqlTest(RequestUriConfigSqlTest test) throws Exception {
        setupConfig(test);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer(test.getTxnName());
        long duration = 501000000;
        startSqlTracer(test.sql, duration).finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        MockRPMService mockRPMService = runHarvest();

        // verify results
        List<SqlTrace> sqlTraces = mockRPMService.getSqlTraces();
        matchUri(test.getExpectedUriValues(), sqlTraces.get(0).getUri());
    }

    public void runTransactionTraceTest(RequestUriConfigTransactionTest test) throws Exception {
        setupConfig(test);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer(test.getTxnName());
        startTracer().finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        MockRPMService mockRPMService = runHarvest();

        // verify results
        List<TransactionTrace> transactionTraces = mockRPMService.getTraces();
        assertFalse(transactionTraces.isEmpty());
        matchUri(test.getExpectedUriValues(), transactionTraces.get(0).getRequestUri());
    }

    public void runTracedErrorTest(RequestUriConfigTransactionTest test) throws Exception {
        setupConfig(test);

        // run a transaction
        Tracer requestDispatcherTracer = startDispatcherTracer(test.getTxnName());
        DefaultTracer defaultTracer = startTracer();
        NewRelicApiImplementation.initialize();
        AgentBridge.publicApi.noticeError("error");
        defaultTracer.finish(Opcodes.RETURN, null);
        requestDispatcherTracer.finish(Opcodes.RETURN, null);

        MockRPMService mockRPMService = runHarvest();

        // verify results
        List<TracedError> errorTraces = mockRPMService.getErrorService().getAndClearTracedErrors();
        assertFalse(errorTraces.isEmpty());
        for (TracedError trace : errorTraces) {
            Writer writer = new StringWriter();
            trace.writeJSONString(writer);
            JSONParser parser = new JSONParser();
            JSONArray parsedError = (JSONArray) parser.parse(writer.toString());
            matchUri(test.getExpectedUriValues(), (String) ((JSONObject) parsedError.get(4)).get("request_uri"));
        }
    }

    private void matchUri(ArrayList<String> expectedUris, String uri) {
        boolean matched = false;
        for (String expectedUri : expectedUris) {
            // expected uri may be null
            if (Objects.equals(expectedUri, uri)) {
                matched = true;
                break;
            }
        }
        assertTrue(matched);
    }

    public void setupConfig(RequestUriConfigTest test) throws Exception {
        Map<String, Object> stagingMap = createStagingMap();
        stagingMap.putAll(test.getConfig());
        createServiceManager(stagingMap);
    }

    public MockRPMService runHarvest() {
        MockHarvestService mockharvestService = (MockHarvestService) ServiceFactory.getHarvestService();
        mockharvestService.runHarvest(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(),
                new StatsEngineImpl());
        return (MockRPMService) ServiceFactory.getRPMService();
    }

    private Tracer startDispatcherTracer(String uri) throws Exception {
        return startDispatcherTracer(ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName(), uri);
    }

    private Tracer startDispatcherTracer(String appName, String uri) throws Exception {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setRequestURI(uri);
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        BasicRequestRootTracer requestDispatcherTracer = new BasicRequestRootTracer(tx, sig, this, httpRequest,
                httpResponse);
        Dispatcher dispatcher = requestDispatcherTracer.createDispatcher();
        tx.getTransactionActivity().tracerStarted(requestDispatcherTracer);
        tx.setDispatcher(dispatcher);
        tx.setApplicationName(ApplicationNamePriority.REQUEST_ATTRIBUTE, appName);
        return requestDispatcherTracer;
    }

    private SqlTracer startSqlTracer(final String sql, final long duration) throws SQLException {
        DummyConnection conn = new DummyConnection();
        Statement statement = conn.createStatement();
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        SqlTracer sqlTracer = new OtherRootSqlTracer(tx, sig, statement, new SimpleMetricNameFormat(null)) {
            @Override
            public long getDuration() {
                return duration;
            }

            @Override
            public Object getSql() {
                return sql;
            }
        };
        sqlTracer.setConnectionFactory(new ConnectionFactory() {
            @Override
            public Connection getConnection() {
                return null;
            }

            @Override
            public DatabaseVendor getDatabaseVendor() {
                return UnknownDatabaseVendor.INSTANCE;
            }
        });
        sqlTracer.setRawSql(sql);
        tx.getTransactionActivity().tracerStarted(sqlTracer);
        return sqlTracer;
    }

    private DefaultTracer startTracer() throws SQLException {
        DummyConnection conn = new DummyConnection();
        Statement statement = conn.createStatement();
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Statement", "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        DefaultTracer tracer = new OtherRootTracer(tx, sig, statement, new SimpleMetricNameFormat("metric", "segment",
                "some uri")) {
            @Override
            public long getDuration() {
                return 100000;
            }
        };
        tx.getTransactionActivity().tracerStarted(tracer);
        return tracer;
    }

    private Map<String, Object> createStagingMap() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "nope.example.invalid");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, "Unit Test");
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.COLLECT_TRACES, true);
        configMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return configMap;
    }

    private void createServiceManager(Map<String, Object> configMap) throws Exception {
        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(configMap);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        serviceManager.setHarvestService(new MockHarvestService());
        serviceManager.setSqlTraceService(new SqlTraceServiceImpl());
        serviceManager.setAttributesService(new AttributesService());
        serviceManager.setNormalizationService(new NormalizationServiceImpl());

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(serviceManager.getConfigService().getDefaultAgentConfig().getApplicationName());
        rpmService.setEverConnected(true);
        rpmService.setIsConnected(true);
        rpmService.setErrorService(new ErrorServiceImpl(serviceManager.getConfigService().getDefaultAgentConfig().getApplicationName()));
        rpmServiceManager.setRPMService(rpmService);
        serviceManager.start();
    }

}
