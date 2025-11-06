/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import com.google.common.collect.Maps;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.environment.AgentIdentity;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/* (non-javadoc)
 * Note: the "beacon" was a predecessor technology for correlated transaction traces with the browser.
 * Some appearances of the term could be changed to "browser" now.
 */

public class ApiTest implements TransactionListener {
    ApiTestHelper apiTestHelper = new ApiTestHelper();
    private static final String CAT_CONFIG_FILE = "configs/cross_app_tracing_test.yml";
    private static final String HIGH_SECURITY_CONFIG_FILE = "configs/high_security_config.yml";
    public static String HOSTNAME = Hostname.getHostname(ServiceFactory.getConfigService().getDefaultAgentConfig());
    private static final ClassLoader CLASS_LOADER = ApiTest.class.getClassLoader();

    @Before
    public void before() {
        apiTestHelper.serviceManager = ServiceFactory.getServiceManager();
        apiTestHelper.tranStats = null;
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        Transaction.clearTransaction();

        ServiceFactory.setServiceManager(apiTestHelper.serviceManager);

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
        errorService.getAndClearTracedErrors();

        ServiceFactory.getStatsService().getStatsEngineForHarvest(null).clear();
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    public EnvironmentHolder setupEnvironmentHolder(String configFile, String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(configFile, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        apiTestHelper.tranStats = transactionStats;
    }

    @Test
    public void testRecordMetricWithNullName() {
        // noop
        NewRelic.recordMetric(null, 666f);
        // noop
        NewRelic.recordMetric("", 666f);
    }

    @Test
    public void testSetRequestAndResponse() {
        TransactionDataList txList = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txList);
        Transaction tx = Transaction.getTransaction();
        OtherRootTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this,
                new SimpleMetricNameFormat("blah"));
        Assert.assertEquals(tracer, tx.getTransactionActivity().tracerStarted(tracer));

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);

        tracer.finish(0, null);

        Assert.assertEquals(1, txList.size());
        Assert.assertEquals("/mytest", txList.get(0).getRequestUri(AgentConfigImpl.ATTRIBUTES));

        TransactionStats stats = apiTestHelper.tranStats;

        ResponseTimeStats dispatcherStats = stats.getUnscopedStats().getOrCreateResponseTimeStats("HttpDispatcher");
        Assert.assertEquals(1, dispatcherStats.getCallCount());
        Assert.assertEquals(1,
                stats.getUnscopedStats().getOrCreateResponseTimeStats("WebTransaction/Uri/mytest").getCallCount());
        Assert.assertEquals(1, stats.getUnscopedStats().getApdexStats("Apdex/Uri/mytest").getApdexSatisfying());
        Assert.assertEquals(1, stats.getUnscopedStats().getApdexStats("Apdex").getApdexSatisfying());
        Assert.assertEquals(1,
                stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.WEB_TRANSACTION).getCallCount());

        Assert.assertEquals(0,
                stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.OTHER_TRANSACTION_ALL).getCallCount());
    }

    @Test
    public void nameTransactionThenSetRequestAndResponse() {
        TransactionDataList txList = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txList);
        Transaction tx = Transaction.getTransaction();
        OtherRootTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this,
                new SimpleMetricNameFormat("blah"));
        Assert.assertEquals(tracer, tx.getTransactionActivity().tracerStarted(tracer));
        tracer.nameTransaction(TransactionNamePriority.CUSTOM_HIGH);

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);

        tracer.finish(0, null);

        Assert.assertEquals("WebTransaction/Custom/test.newrelic.test.agent.api.ApiTest/",
                tx.getPriorityTransactionName().getName());
    }

    @Test
    public void setRequestAndResponseFirstWins() throws Exception {
        TransactionDataList txList = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txList);
        Transaction tx = Transaction.getTransaction();

        OtherRootTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this,
                new SimpleMetricNameFormat("otherRootTracer"));
        Assert.assertEquals(tracer, tx.getTransactionActivity().tracerStarted(tracer));

        Request firstRequest = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "firstWins", "", ""));
        Response firstResponse = new FirstResponse(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(firstRequest, firstResponse);

        Request secondRequest = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "thisIsABug", "", ""));
        Response secondResponse = new SecondResponse(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(secondRequest, secondResponse);

        tracer.finish(0, null);

        Assert.assertEquals(firstRequest.getRequestURI(), tx.getDispatcher().getUri());
        Assert.assertEquals(firstResponse.getStatus(), ((WebRequestDispatcher) tx.getDispatcher()).getStatus());

        String name = tx.getPriorityTransactionName().getName();
        Assert.assertNotEquals("WebTransaction/Uri/thisIsABug", name);
        Assert.assertEquals("WebTransaction/Uri/firstWins", name);
    }

    private static class FirstResponse extends ApiTestHelper.ResponseWrapper {

        public FirstResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public int getStatus() {
            return 200;
        }
    }

    private static class SecondResponse extends ApiTestHelper.ResponseWrapper {

        public SecondResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public int getStatus() {
            return 404;
        }
    }

    @Test
    public void setRequestAndResponseNullRequestResponse() {
        TransactionDataList txList = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txList);
        Transaction tx = Transaction.getTransaction();

        OtherRootTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this,
                new SimpleMetricNameFormat("otherRootTracer"));
        Assert.assertEquals(tracer, tx.getTransactionActivity().tracerStarted(tracer));

        NewRelic.setRequestAndResponse(null, null);
        Assert.assertFalse(tx.isWebRequestSet());
        Assert.assertFalse(tx.isWebResponseSet());
        tracer.finish(0, null);
        Assert.assertTrue(tx.isWebTransaction());
    }

    @Test
    public void testSetTxNameThenSetRequestAndResponse() {
        TransactionDataList txList = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txList);
        Transaction tx = Transaction.getTransaction();
        OtherRootTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this,
                new SimpleMetricNameFormat("blah"));
        Assert.assertEquals(tracer, tx.getTransactionActivity().tracerStarted(tracer));

        NewRelic.setTransactionName("Test", "Foo");

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);

        tracer.finish(0, null);

        Assert.assertEquals("WebTransaction/Test/Foo", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testSetRequestAndResponseThenSetTxName() {
        TransactionDataList txList = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txList);
        Transaction tx = Transaction.getTransaction();
        OtherRootTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this,
                new SimpleMetricNameFormat("blah"));
        Assert.assertEquals(tracer, tx.getTransactionActivity().tracerStarted(tracer));

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);
        NewRelic.setTransactionName("Test", "Foo");

        tracer.finish(0, null);

        Assert.assertEquals("WebTransaction/Test/Foo", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testRecordResponseTimeMetricWithNullName() {
        // noop
        NewRelic.recordResponseTimeMetric(null, 1500000);
        // noop
        NewRelic.recordResponseTimeMetric("", 1500000);
    }

    @Test
    public void testSetUserName() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);

        NewRelic.setUserName("myname");
        Assert.assertEquals("myname", tx.getUserAttributes().get("user"));
    }

    @Test
    public void testSetAccountName() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);

        NewRelic.setAccountName("accountName");
        Assert.assertEquals("accountName", tx.getUserAttributes().get("account"));
    }

    @Test
    public void testSetProductName() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));

        Request request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ApiTestHelper.ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);

        NewRelic.setProductName("prodName");
        Assert.assertEquals("prodName", tx.getUserAttributes().get("product"));
    }

    @Test
    public void testSetTransactionName() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        NewRelic.setTransactionName("Test", "/foo");
        Assert.assertEquals("OtherTransaction/Test/foo", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testSetTransactionNameMoreThanOnce() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        NewRelic.setTransactionName("Test", "/foo");
        NewRelic.setTransactionName("Test", "/dude");
        NewRelic.setTransactionName("Test", "/bar");
        Assert.assertEquals("OtherTransaction/Test/bar", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testSetTransactionNameNoCategory() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        NewRelic.setTransactionName(null, "/foo");
        Assert.assertEquals("OtherTransaction/" + MetricNames.CUSTOM + "/foo",
                tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testSetTransactionNameNoStartingForwardSlash() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        NewRelic.setTransactionName("Test", "foo");
        Assert.assertEquals("OtherTransaction/Test/foo", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testSetTransactionNameAfterGetBrowserInstrumentationScript() throws Exception {

        ApiTestHelper.mockOutServiceManager();

        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));

        NewRelic.setTransactionName("Test", "/foo1");

        NewRelic.getBrowserTimingHeader();

        NewRelic.setTransactionName("Test", "/foo2");

        // still foo1 as the name has been locked and the second setting of the name is ignored (and a message is logged)
        Assert.assertEquals("OtherTransaction/Test/foo1", tx.getPriorityTransactionName().getName());
    }

    @Test
    public void testAddCustomNumberParameter() {
        try {
            runTestAddCustomNumberParameter();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomNumberParameter() {
        NewRelic.addCustomParameter("num", 5);
        Assert.assertEquals(5, Transaction.getTransaction().getUserAttributes().get("num"));
    }

    @Test
    public void testAddCustomNumberParameterWithNull() {
        try {
            runTestAddCustomNmberParameterWithNull();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomNmberParameterWithNull() {
        NewRelic.addCustomParameter("num", 5);
        // this should be a noop
        NewRelic.addCustomParameter("num", (Number) null);

        Assert.assertEquals(5, Transaction.getTransaction().getUserAttributes().get("num"));
    }

    @Test
    public void testAddCustomNumberParameterOverLimit() {
        try {
            runTestAddCustomNumberParameterOverLimit();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomNumberParameterOverLimit() {
        int maxSize = ConfigConstant.MAX_USER_ATTRIBUTES;
        for (int i = 0; i < maxSize; i++) {
            NewRelic.addCustomParameter("str" + i, i);
        }
        Assert.assertEquals(maxSize, Transaction.getTransaction().getUserAttributes().size());
        NewRelic.addCustomParameter("str", 1);
        Assert.assertEquals(maxSize, Transaction.getTransaction().getUserAttributes().size());
        Assert.assertNull(Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomStringParameter() {
        try {
            runTestAddCustomStringParameter();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameter() {
        NewRelic.addCustomParameter("str", "dude");
        Assert.assertEquals("dude", Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomStringParameterWithNull() {
        try {
            runTestAddCustomStringParameterWithNull();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameterWithNull() {
        NewRelic.addCustomParameter("str", "some string");
        // this should be a noop
        NewRelic.addCustomParameter("str", (String) null);
        Assert.assertEquals("some string", Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomStringParameterWithEmpty() {
        try {
            runTestAddCustomStringParameterWithEmpty();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameterWithEmpty() {
        NewRelic.addCustomParameter("str", "some string");
        NewRelic.addCustomParameter("str", "");

        Assert.assertEquals("", Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomStringParameterTooBig() {
        try {
            runTestAddCustomStringParameterTooBig();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameterTooBig() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(255);
        for (int i = 0; i < maxParamSize; i++) {
            sb.append('A');
        }
        sb.append('A');
        sb.append('A');

        String val = sb.toString();
        NewRelic.addCustomParameter("str", val);
        Assert.assertTrue(((String) Transaction.getTransaction().getUserAttributes().get("str")).length() < val.length());
        Assert.assertEquals(sb.substring(0, 255), Transaction.getTransaction().getUserAttributes().get("str"));

    }

    @Test
    public void testAddCustomStringUTFParameterTooBig() {
        try {
            runTestAddCustomStringUTFParameterTooBig();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringUTFParameterTooBig() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(258);
        for (int i = 0; i < (maxParamSize - 2); i++) {
            sb.append('A');
        }
        sb.append("\u00EB");
        sb.append('B');
        sb.append('B');
        sb.append('B');
        sb.append('B');

        Assert.assertEquals(sb.length(), 258);
        String val = sb.toString();
        NewRelic.addCustomParameter("str", val);
        Assert.assertTrue(((String) Transaction.getTransaction().getUserAttributes().get("str")).length() < val.length());
        Assert.assertEquals(254, ((String) Transaction.getTransaction().getUserAttributes().get("str")).length());
        byte[] byteVal = val.getBytes(StandardCharsets.UTF_8);
        String trimmedVal = new String(Arrays.copyOf(byteVal, maxParamSize), StandardCharsets.UTF_8);
        Assert.assertEquals(trimmedVal, Transaction.getTransaction().getUserAttributes().get("str"));
        Assert.assertEquals(trimmedVal.length(), 254);
        Assert.assertEquals(((String) Transaction.getTransaction().getUserAttributes().get("str")).length(), 254);
    }

    @Test
    public void testAddCustomUTFStringParameterValueTooBig() {
        try {
            runTestAddCustomStringParameterValueTooBigUTF2Bytes();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameterValueTooBigUTF2Bytes() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(255);
        sb.append("ë");
        sb.append("ë");
        sb.append("\u00EB");
        for (int i = 0; i < maxParamSize; i++) {
            sb.append('A');
        }
        String val = sb.toString();
        NewRelic.addCustomParameter("str", val);
        int valueLength = ((String) Transaction.getTransaction().getUserAttributes().get("str")).length();
        Assert.assertTrue(valueLength < val.length());
        // length must be 253 since each Unicode character ë requires 2 bytes for encoding in UTF-8
        Assert.assertEquals(valueLength, 252);
        Assert.assertEquals(sb.substring(0, 252), Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomUStringParameterValueTooBigTF3Bytes() {
        try {
            runTestAddCustomStringParameterValueTooBigUTF3Bytes();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    public void runTestAddCustomStringParameterValueTooBigUTF3Bytes() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(256);
        // Add 253 characters to the StringBuilder sb
        for (int i = 0; i < (maxParamSize - 2); i++) {
            sb.append('A');
        }
        sb.append('\u2941');
        sb.append('A');

        String val = sb.toString();
        // make sure the value string has the limit size in number of unicode characters
        Assert.assertEquals(val.length(), 255);
        NewRelic.addCustomParameter("str", val);
        int valueLength = ((String) Transaction.getTransaction().getUserAttributes().get("str")).length();
        // since the Unicode character \u2941 requires 3 bytes, the value length is truncated to the
        // first 253 characters
        Assert.assertEquals(253, valueLength);
        Assert.assertEquals(sb.substring(0, 253), Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomUStringParameterValueTooBigSurrogate() {
        try {
            runTestAddCustomStringParameterValueTooBigSurrogate();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    public void runTestAddCustomStringParameterValueTooBigSurrogate() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(256);
        // Add 253 characters to the StringBuilder sb
        for (int i = 0; i < (maxParamSize - 4); i++) {
            sb.append('A');
        }
        // Add a surrogate character, which adds two characters to sb
        sb.append("\uD83D\uDF01");
        sb.append('A');

        String val = sb.toString();
        Assert.assertEquals(val.length(), 254);
        NewRelic.addCustomParameter("str", val);
        int valueLength = ((String) Transaction.getTransaction().getUserAttributes().get("str")).length();
        Assert.assertEquals(253, valueLength);
        Assert.assertEquals('\uDF01',
                Transaction.getTransaction().getUserAttributes().get("str").toString().charAt(252));
    }

    @Test
    public void testAddCustomStringParameterKeyTooBig() {
        try {
            runTestAddCustomStringParameterKeyTooBig();

        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameterKeyTooBig() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(255);
        for (int i = 0; i < maxParamSize; i++) {
            sb.append('A');
        }
        sb.append('A');
        String key = sb.toString();

        int userAttSize = Transaction.getTransaction().getUserAttributes().size();
        NewRelic.addCustomParameter(key, "str");

        Assert.assertNull(Transaction.getTransaction().getUserAttributes().get(key));
        Assert.assertEquals(userAttSize, Transaction.getTransaction().getUserAttributes().size());
    }

    @Test
    public void testAddCustomUTFStringParameterKeyTooBig() {
        try {
            runTestAddCustomUTFStringParameterKeyTooBig();

        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomUTFStringParameterKeyTooBig() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(255);
        for (int i = 0; i < (maxParamSize - 2); i++) {
            sb.append('A');
        }
        sb.append("ë");
        String key = sb.toString();

        int userAttSize = Transaction.getTransaction().getUserAttributes().size();
        NewRelic.addCustomParameter(key, "str");
        Assert.assertTrue(key.getBytes(StandardCharsets.UTF_8).length > maxParamSize);
        Assert.assertNull(Transaction.getTransaction().getUserAttributes().get(key));
        Assert.assertEquals(userAttSize, Transaction.getTransaction().getUserAttributes().size());
    }

    @Test
    public void testAddCustomUTFStringParameterKeyLimits() {
        try {
            runTestAddCustomUTFStringParameterKeyLimits();

        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomUTFStringParameterKeyLimits() {
        int maxParamSize = ConfigConstant.MAX_USER_ATTRIBUTE_SIZE;
        StringBuilder sb = new StringBuilder(255);
        for (int i = 0; i < maxParamSize - 3; i++) {
            sb.append('A');
        }
        sb.append("\u0065");
        sb.append("\u0308");
        String key = sb.toString();

        int userAttSize = Transaction.getTransaction().getUserAttributes().size();
        NewRelic.addCustomParameter(key, "str");
        // the string length does not consider the UTF-8 encoding, so the next assertion should be true
        Assert.assertTrue(key.length() < maxParamSize);
        // make sure we are testing the limits of the key parameter, and taking into consideration the UTF-8 byte
        // encoding
        Assert.assertEquals((key.getBytes(StandardCharsets.UTF_8)).length, maxParamSize);
        Assert.assertNotNull(Transaction.getTransaction().getUserAttributes().get(key));
        Assert.assertEquals(userAttSize + 1, Transaction.getTransaction().getUserAttributes().size());

        // attempt to insert the null key
        NewRelic.addCustomParameter(null, "str");
        // the number of attributes should be unchanged
        Assert.assertEquals(userAttSize + 1, Transaction.getTransaction().getUserAttributes().size());
    }

    @Test
    public void testAddCustomStringParameterOverLimit() {
        try {
            runTestAddCustomStringParameterOverLimit();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomStringParameterOverLimit() {
        int maxSize = ConfigConstant.MAX_USER_ATTRIBUTES;
        for (int i = 0; i < maxSize; i++) {
            NewRelic.addCustomParameter("str" + i, "dude");
        }
        Assert.assertEquals(maxSize, Transaction.getTransaction().getUserAttributes().size());
        NewRelic.addCustomParameter("str", "dude");
        Assert.assertEquals(maxSize, Transaction.getTransaction().getUserAttributes().size());
        Assert.assertNull(Transaction.getTransaction().getUserAttributes().get("str"));
    }

    @Test
    public void testAddCustomBoolParameter() {
        try {
            runTestAddCustomBoolParameter();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestAddCustomBoolParameter() {
        NewRelic.addCustomParameter("bool", true);
        Assert.assertEquals(true, Transaction.getTransaction().getUserAttributes().get("bool"));
    }


    @Test
    public void testSetUserId() throws Exception {
        EnvironmentHolder holder = setupEnvironmentHolder(HIGH_SECURITY_CONFIG_FILE, "high_security_disabled_test");
        try {
            runTestSetUserId();
        } finally {
            Transaction.clearTransaction();
            holder.close();
        }
    }


    @Trace(dispatcher = true)
    private void runTestSetUserId() {
        NewRelic.setUserId("hello");
        Assert.assertEquals("hello", Transaction.getTransaction().getAgentAttributes().get("enduser.id"));
        NewRelic.setUserId("");
        Assert.assertFalse("Agent attributes shouldn't have user ID", Transaction.getTransaction().getAgentAttributes().containsKey("enduser.id"));
        NewRelic.setUserId(null);
        Assert.assertFalse("Agent attributes shouldn't have user ID", Transaction.getTransaction().getAgentAttributes().containsKey("enduser.id"));
    }

    @Test
    public void testSetUserIdWithHighSecurity() throws Exception {
        EnvironmentHolder holder = setupEnvironmentHolder(HIGH_SECURITY_CONFIG_FILE, "high_security_enabled_test");
        try {
            runTestSetUserId();
        } finally {
            Transaction.clearTransaction();
            holder.close();
        }
    }


    @Trace(dispatcher = true)
    private void runTestSetUserWithHighSecurity() {
        NewRelic.setUserId("hello");
        Assert.assertFalse("Agent attributes shouldn't have user ID", Transaction.getTransaction().getAgentAttributes().containsKey("enduser.id"));
    }

    @Test
    public void testIgnoreApdexNotSet() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(createDispatcherTracer());
        Assert.assertTrue(tx.getRootTracer() instanceof TransactionActivityInitiator);
        Assert.assertTrue("Tracer should not be ignored for apdex", !tx.getDispatcher().isIgnoreApdex());
    }

    @Test
    public void testIgnoreApdex() {
        Transaction tx = Transaction.getTransaction();
        Tracer init = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(init);
        tx.startTransactionIfBeginning(init);
        NewRelic.ignoreApdex();
        Assert.assertTrue(tx.getRootTracer() instanceof TransactionActivityInitiator);
        Assert.assertTrue("Tracer should be ignored for apdex", tx.getDispatcher().isIgnoreApdex());
    }

    @Test
    public void testIgnoreTransaction() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        NewRelic.ignoreTransaction();
        Assert.assertTrue("Transaction should be ignored", tx.isIgnore());
    }

    @Test
    public void testIgnoreTransactionNotSet() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        Assert.assertTrue("Transaction should be ignored", !tx.isIgnore());
    }

    @Test
    public void testIgnoreTransactionNoTracer() {
        Transaction tx = Transaction.getTransaction();
        NewRelic.ignoreTransaction();
        Assert.assertTrue("Transaction should not be ignored with no tracer set", !tx.isIgnore());
    }

    @Test
    public void testIncrementCounterNullAndEmpty() {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(createDispatcherTracer());
        // Both should just log with no other side effect
        NewRelic.incrementCounter(null);
        NewRelic.incrementCounter("");
    }

    @Test
    public void testNoticeErrorWithNullOutsideTransaction() throws Exception {

        ApiTestHelper.mockOutServiceManager();

        NewRelic.noticeError((Throwable) null);
        NewRelic.noticeError((Throwable) null);
        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
        Assert.assertEquals("incorrect error count", 2, errorService.getAndClearTracedErrors().size());
    }

    @Test
    public void testNoticeErrorOutsideTransaction() throws Exception {

        ApiTestHelper.mockOutServiceManager();

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();

        NewRelic.noticeError(new RuntimeException("boom"));
        try { // ensure that the ThrowableErrors have different timestamps
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        NewRelic.noticeError(new RuntimeException("boom2"));

        Collection<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals("incorrect traced errors count: " + tracedErrors.toString(), 2, tracedErrors.size());
        TracedError tracedError = (TracedError) tracedErrors.toArray()[0];
        Assert.assertEquals("exception class incorrect", "java.lang.RuntimeException", tracedError.getExceptionClass());
        Assert.assertEquals("error message incorrect", "boom", tracedError.getMessage());
    }

    @Test
    public void testNoticeErrorMsgParamsOutsideTransaction() throws Exception {

        ApiTestHelper.mockOutServiceManager();

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();

        Map<String, String> atts = new HashMap<>();
        atts.put("str", "dude");

        NewRelic.noticeError("outside1", atts);
        try { // ensure that the errors have different timestamps
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        NewRelic.noticeError("outside2", new HashMap<String, String>());

        Collection<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals("incorrect traced errors count", 2, tracedErrors.size());
        TracedError tracedError = (TracedError) tracedErrors.toArray()[0];
        Assert.assertEquals("error attribute incorrect", "dude", tracedError.getErrorAtts().get("str"));
        // getExceptionClass on a HTTPTracedError is getMessage
        Assert.assertEquals("exception class incorrect", "outside1", tracedError.getExceptionClass());
        Assert.assertEquals("error message incorrect", "outside1", tracedError.getMessage());
    }

    @Test
    public void testNoticeErrorThrowableParamsOutsideTransaction() throws Exception {

        ApiTestHelper.mockOutServiceManager();

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
        Map<String, String> atts = new HashMap<>();

        atts.put("str", "dude");
        NewRelic.noticeError(new RuntimeException("boom"), atts);
        try { // ensure that the errors have different timestamps
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        NewRelic.noticeError(new RuntimeException("boom2"), new HashMap<String, String>());

        Collection<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals("incorrect traced errors count", 2, tracedErrors.size());

        TracedError tracedError = (TracedError) tracedErrors.toArray()[0];

        Assert.assertEquals("error attribute incorrect", "dude", tracedError.getErrorAtts().get("str"));
        Assert.assertEquals("exception class incorrect", "java.lang.RuntimeException", tracedError.getExceptionClass());
        Assert.assertEquals("error message incorrect", "boom", tracedError.getMessage());
    }

    @Test
    public void testNoticeErrorThrowableObjectParamsInTransaction() throws Exception {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, Object> atts = new HashMap<>();
            atts.put("one", 5);
            atts.put("three", 6);

            NewRelic.noticeError(new RuntimeException("boom"), atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(2, tx.getErrorAttributes().size());
            Assert.assertEquals(5, tx.getErrorAttributes().get("one"));
            Assert.assertEquals(6, tx.getErrorAttributes().get("three"));
        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeErrorInsideTransaction() {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.noticeError(new RuntimeException("boom"));
        NewRelic.noticeError(new RuntimeException("boom2"));

        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertNotNull(transactionThrowable.throwable);
            Assert.assertEquals("boom2", transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoticeErrorMsgParamsInsideTransaction() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.noticeError("boom", new HashMap<String, String>());

        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoticeErrorWithParams() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = new HashMap<>();
            atts.put("one", "two");
            atts.put("three", "four");

            NewRelic.noticeError("boom", atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(2, tx.getErrorAttributes().size());
            Assert.assertEquals("two", tx.getErrorAttributes().get("one"));
            Assert.assertEquals("four", tx.getErrorAttributes().get("three"));
        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeErrorWithParamsObjects() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, Object> atts = new HashMap<>();
            atts.put("one", 5);
            atts.put("three", 6);

            NewRelic.noticeError("boom", atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(2, tx.getErrorAttributes().size());
            Assert.assertEquals(5, tx.getErrorAttributes().get("one"));
            Assert.assertEquals(6, tx.getErrorAttributes().get("three"));
        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeErrorWithParamsKeyTooLarge() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 258; i++) {
                sb.append("A");
            }
            atts.put(sb.toString(), "two");
            atts.put("three", "four");

            NewRelic.noticeError("boom", atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(1, tx.getErrorAttributes().size());
            Assert.assertNull(tx.getErrorAttributes().get("one"));
            Assert.assertEquals("four", tx.getErrorAttributes().get("three"));

        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeErrorWithParamsValueTooLarge() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 258; i++) {
                sb.append("A");
            }
            atts.put("one", sb.toString());
            atts.put("three", "four");

            NewRelic.noticeError("boom", atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(2, tx.getErrorAttributes().size());
            Assert.assertEquals(sb.substring(0, 255), tx.getErrorAttributes().get("one"));
            Assert.assertEquals("four", tx.getErrorAttributes().get("three"));

        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeErrorWithTooManyParams() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = Maps.newHashMapWithExpectedSize(100);
            for (int i = 0; i < 70; i++) {
                atts.put("key" + i, "value");
            }

            NewRelic.noticeError("boom", atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(64, tx.getErrorAttributes().size());

        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeThrowableWithParamsKeyTooLarge() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 258; i++) {
                sb.append("A");
            }
            atts.put(sb.toString(), "two");
            atts.put("three", "four");
            Throwable t = new Throwable("test");
            NewRelic.noticeError(t, atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("test",transactionThrowable.throwable.getMessage());
            Assert.assertEquals(1, tx.getErrorAttributes().size());
            Assert.assertNull(tx.getErrorAttributes().get("one"));
            Assert.assertEquals("four", tx.getErrorAttributes().get("three"));

        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeThrowableWithParamsValueTooLarge() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 258; i++) {
                sb.append("A");
            }
            atts.put("one", sb.toString());
            atts.put("three", "four");

            Throwable t = new Throwable("test");
            NewRelic.noticeError(t, atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("test", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(2, tx.getErrorAttributes().size());
            Assert.assertEquals(sb.substring(0, 255), tx.getErrorAttributes().get("one"));
            Assert.assertEquals("four", tx.getErrorAttributes().get("three"));

        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeThrowableWithTooManyParams() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        try {
            tx.getTransactionActivity().tracerStarted(tracer);

            Map<String, String> atts = Maps.newHashMapWithExpectedSize(100);
            for (int i = 0; i < 70; i++) {
                atts.put("key" + i, "value");
            }

            Throwable t = new Throwable("test");
            NewRelic.noticeError(t, atts);

            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("test", transactionThrowable.throwable.getMessage());
            Assert.assertEquals(64, tx.getErrorAttributes().size());

        } finally {
            tx.getTransactionActivity().tracerFinished(tracer, 0);
        }
    }

    @Test
    public void testNoticeErrorNullMsgParamsInsideTransaction() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.noticeError((String) null, new HashMap<String, String>());
        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertNull(transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoticeErrorMsgNullParamsInsideTransaction() {

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
        errorService.getAndClearTracedErrors();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.noticeError("paramsAreNull", null);
        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("paramsAreNull", transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoticeExpectedError() {
        Throwable expectedThrowable = new Throwable("Session timed out. Don't page people");
        com.newrelic.api.agent.NewRelic.noticeError(expectedThrowable, true);

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
        List<TracedError> errors = errorService.getAndClearTracedErrors();
        TracedError tracedError = errors.get(0);
        Assert.assertEquals("Session timed out. Don't page people", tracedError.getMessage());
        Assert.assertFalse(tracedError.incrementsErrorMetric());

        Throwable unexpectedThrowable = new Throwable("Surprise surprise, the database is down!");
        com.newrelic.api.agent.NewRelic.noticeError(unexpectedThrowable, false);

        errors = errorService.getAndClearTracedErrors();
        tracedError = errors.get(0);
        Assert.assertEquals("Surprise surprise, the database is down!", tracedError.getMessage());
        Assert.assertTrue(tracedError.incrementsErrorMetric());
    }

    @Test
    public void testGetBrowserTimingHeader() throws Exception {
        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();

        // The only thing that can vary in the returned script is the "applicationTime" property, so we remove it
        Pattern p = Pattern.compile("\"applicationTime\":([0-9]*)");
        browserTimingHeader = browserTimingHeader.replaceFirst("\"applicationTime\":([0-9]*)", "");


        Assert.assertEquals("Incorrect header", ApiTestHelper.JAVASCRIPT_AGENT_SCRIPT, browserTimingHeader);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testGetBrowserTimingHeaderWhenRUMEnabledNotSpecified() throws Exception {

        Map<String, Object> connectionResponse = new HashMap<>();
        connectionResponse.put(ApiTestHelper.BROWSER_KEY, "abcd");
        connectionResponse.put(ApiTestHelper.BROWSER_LOADER_VERSION, "248");
        connectionResponse.put(ApiTestHelper.JS_AGENT_LOADER, ApiTestHelper.LOADER);
        connectionResponse.put(ApiTestHelper.JS_AGENT_FILE, "js-agent.newrelic.com\nr-248.min.js");
        connectionResponse.put(ApiTestHelper.BEACON, "staging-beacon-2.newrelic.com");
        connectionResponse.put(ApiTestHelper.ERROR_BEACON, "staging-jserror.newrelic.com");
        connectionResponse.put(ApiTestHelper.APPLICATION_ID, 100L);

        // specifically not specified...should default to true
        // connectionResponse.put(RUM_ENABLED_KEY, false);
        ApiTestHelper.mockOutServiceManager(connectionResponse);

        Transaction tx = Transaction.getTransaction();

        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();

        // The only thing that can vary in the returned script is the "applicationTime" property, so we remove it
        Pattern p = Pattern.compile("\"applicationTime\":([0-9]*)");
        browserTimingHeader = browserTimingHeader.replaceFirst("\"applicationTime\":([0-9]*)", "");

        Assert.assertEquals("Incorrect header", ApiTestHelper.JAVASCRIPT_AGENT_SCRIPT, browserTimingHeader);
        tx.getTransactionActivity().tracerFinished(tracer, 0);

    }

    @Test
    public void testGetBrowserTimingHeaderWhenRUMDisabled() throws Exception {

        Map<String, Object> connectionResponse = new HashMap<>();
        ApiTestHelper.mockOutServiceManager(connectionResponse);

        Transaction tx = Transaction.getTransaction();

        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();
        Assert.assertEquals("Incorrect header", "", browserTimingHeader);
        tx.getTransactionActivity().tracerFinished(tracer, 0);

    }

    @Test
    public void testGetBrowserTimingHeaderTwice() throws Exception {

        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();
        browserTimingHeader = NewRelic.getBrowserTimingHeader();
        Assert.assertEquals("Incorrect header", "", browserTimingHeader);
        tx.getTransactionActivity().tracerFinished(tracer, 0);

    }

    @Test
    public void testGetBrowserTimingHeaderForIgnoredTransaction() throws Exception {

        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        tx.setIgnore(true);

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();
        Assert.assertEquals("Incorrect header", "", browserTimingHeader);
        tx.getTransactionActivity().tracerFinished(tracer, 0);

    }

    @Test
    public void testGetBrowserTimingHeaderNoBeaconConfiguration() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();
        Assert.assertEquals("No beacon configuration, the timing header should be empty", "", browserTimingHeader);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testGetBrowserTimingHeaderNoTransaction() {

        String browserTimingHeader = NewRelic.getBrowserTimingHeader();
        Assert.assertEquals("Outside of a transaction, the timing header should be empty", "", browserTimingHeader);
    }
    
    @Test
    public void testRUMWithNoBeacon() throws Exception {

        ApiTestHelper.mockOutServiceManager(false, true, true, true, true, true);
        Assert.assertNull(ServiceFactory.getBeaconService().getBrowserConfig(null));

    }

    @Test
    public void testRUMWithNoErrorBeacon() throws Exception {

        ApiTestHelper.mockOutServiceManager(true, false, true, true, true, true);
        Assert.assertNull(ServiceFactory.getBeaconService().getBrowserConfig(null));
    }

    @Test
    public void testRUMWithNoJsAgentLoader() throws Exception {

        ApiTestHelper.mockOutServiceManager(true, true, false, true, true, true);
        Assert.assertNull(ServiceFactory.getBeaconService().getBrowserConfig(null));
    }

    @Test
    public void testRUMWithNoJsAgentFile() throws Exception {
        ApiTestHelper.mockOutServiceManager(true, true, true, false, true, true);
        Assert.assertNull(ServiceFactory.getBeaconService().getBrowserConfig(null));
    }

    @Test
    public void testRUMWithNoBrowerKey() throws Exception {
        ApiTestHelper.mockOutServiceManager(true, true, true, true, false, true);
        Assert.assertNull(ServiceFactory.getBeaconService().getBrowserConfig(null));
    }

    @Test
    public void testRUMWithNoApplicationId() throws Exception {
        ApiTestHelper.mockOutServiceManager(true, true, true, true, true, false);
        Assert.assertNull(ServiceFactory.getBeaconService().getBrowserConfig(null));
    }

    @Test
    public void testGetBrowserTimingFooterTwice() throws Exception {
        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.getBrowserTimingHeader();
        String browserTimingFooter = NewRelic.getBrowserTimingFooter();
        browserTimingFooter = NewRelic.getBrowserTimingFooter();
        Assert.assertEquals("Incorrect header", "", browserTimingFooter);
        tx.getTransactionActivity().tracerFinished(tracer, 0);

    }

    @Test
    public void testGetBrowserTimingFooterNoHeader() throws Exception {

        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingFooter = NewRelic.getBrowserTimingFooter();
        Assert.assertEquals("Incorrect header", "", browserTimingFooter);
        tx.getTransactionActivity().tracerFinished(tracer, 0);

    }

    @Test
    public void testGetBrowserTimingFooterWhenRUMDisabled() throws Exception {
        Map<String, Object> connectionResponse = new HashMap<>();

        ApiTestHelper.mockOutServiceManager(connectionResponse);

        Transaction tx = Transaction.getTransaction();

        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.getBrowserTimingHeader();
        String browserTimingFooter = NewRelic.getBrowserTimingFooter();
        Assert.assertEquals("Incorrect header", "", browserTimingFooter);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testGetBrowserTimingFooterForIgnoredTransaction() throws Exception {
        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        tx.setIgnore(true);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingFooter = NewRelic.getBrowserTimingFooter();
        Assert.assertEquals("Incorrect footer", "", browserTimingFooter);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testGetBrowserTimingFooterNoBeaconConfiguration() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        String browserTimingFooter = NewRelic.getBrowserTimingFooter();
        Assert.assertEquals("No beacon configuration, the timing footer should be empty", "", browserTimingFooter);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testGetBrowserTimingShortFooterForIgnoredTransaction() throws Exception {

        ApiTestHelper.mockOutServiceManager();
        Transaction tx = Transaction.getTransaction();
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        tx.setIgnore(true);

        NewRelic.getBrowserTimingHeader();
        String browserTimingFooter = NewRelic.getBrowserTimingFooter();
        Assert.assertEquals("Incorrect short header", "", browserTimingFooter);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testGetBrowserTimingFooterRUM4() throws Exception {
        // The getBrowserTimingFooter API is deprecated and now only returns an empty String
        Assert.assertEquals("", NewRelic.getBrowserTimingFooter());
        Assert.assertEquals("", NewRelic.getBrowserTimingFooter("123"));
    }

    private String getTimingFooterStart() {
        return "\n<script type=\"text/javascript\">window.NREUM||(NREUM={});NREUM.info={";
    }

    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
    }

    @Test
    public void testNoticeErrorThrowableNullParamsInsideTransaction() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();

        NewRelic.noticeError("paramsAreNull", null);

        // in a transaction so does not show in the error service
        Assert.assertEquals("Incorrect error count", 0, errorService.getAndClearTracedErrors().size());

        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("paramsAreNull", transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoticeErrorThrowableParamsInsideTransaction() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.noticeError("boom", new HashMap<String, String>());

        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertEquals("boom", transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoticeErrorNullThrowableParamsInsideTransaction() {

        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);

        NewRelic.noticeError((String) null, new HashMap<String, String>());

        try {
            Class<? extends Transaction> c = tx.getClass();
            Method m = c.getDeclaredMethod("getThrowable");
            TransactionThrowable transactionThrowable = (TransactionThrowable) m.invoke(tx);
            Assert.assertNull(transactionThrowable.throwable.getMessage());
        } catch (SecurityException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException e) {
        }

        tx.getTransactionActivity().tracerFinished(tracer, 0);
    }

    @Test
    public void testNoExceptionRecordMetric() throws Exception {
        Transaction tx = Transaction.getTransaction();
        tx.getTransactionActivity().tracerStarted(
                new OtherRootTracer(tx, new ClassMethodSignature("", "", ""), this, new SimpleMetricNameFormat("dude")));
        String name = "roger";
        tx.getTransactionActivity().getTransactionStats().getUnscopedStats().getApdexStats(name);

        try {
            tx.getTransactionActivity().getTransactionStats().getUnscopedStats().getStats(name);
            Assert.fail("expected java.lang.RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }
        NewRelic.recordMetric("roger", 1.0f);
    }

    @Test
    public void testNoExceptionRecordMetricNoTransaction() throws Exception {
        final MetricName name = MetricName.create("roger");
        StatsWork statsWork = new StatsWork() {

            @Override
            public void doWork(StatsEngine statsEngine) {
                statsEngine.getApdexStats(name);
                try {
                    statsEngine.getStats(name);
                    Assert.fail("expected java.lang.RuntimeException");
                } catch (RuntimeException e) {
                    // expected
                }
            }

            @Override
            public String getAppName() {
                return null;
            }
        };
        ServiceFactory.getStatsService().doStatsWork(statsWork, "statsWorkName" );

        NewRelic.recordMetric(name.getName(), 1.0f);
    }

    @Test
    public void testSetIgnore() {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        NewRelic.ignoreTransaction();
        Assert.assertEquals(Transaction.getTransaction().isIgnore(), false);
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        Assert.assertEquals(Transaction.getTransaction().isIgnore(), false);
        NewRelic.ignoreTransaction();
        Assert.assertEquals(Transaction.getTransaction().isIgnore(), true);

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        tx.setIgnore(true);
        Assert.assertEquals(Transaction.getTransaction().isIgnore(), false);
        tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        Assert.assertEquals(Transaction.getTransaction().isIgnore(), false);
        tx.setIgnore(true);
        Assert.assertEquals(Transaction.getTransaction().isIgnore(), true);
    }

    /* Web Frameworks - FIT to Public API */

    @Test
    public void testWebFrameworkAPI() {
        try {
            runTestWebFrameworkAPI();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestWebFrameworkAPI() {
        NewRelic.setAppServerPort(666);
        NewRelic.setInstanceName("instance");
        NewRelic.setServerInfo("server", "6.6.6");

        com.newrelic.agent.bridge.Transaction txn = AgentBridge.getAgent().getTransaction();

        AgentIdentity env = ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity();
        txn.convertToWebTransaction();
        Assert.assertTrue(txn.isWebTransaction());

        ExtendedRequest request = new ApiTestHelper.RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        txn.setWebRequest(request);
        Assert.assertTrue(txn.isWebRequestSet());

        ApiTestHelper.DummyResponse dummyResponse = new ApiTestHelper.DummyResponse();
        NewRelic.getAgent().getTransaction().setWebResponse(dummyResponse);
        Assert.assertTrue(txn.isWebResponseSet());

        WebRequestDispatcher webRequestDispatcher = (WebRequestDispatcher) AgentBridge.getAgent().getTransaction().getWebResponse();

        try {
            int status = webRequestDispatcher.getResponse().getStatus();
            String statusMessage = webRequestDispatcher.getResponse().getStatusMessage();
            Assert.assertEquals(200, status);
            Assert.assertEquals("HTTP 200 OK", statusMessage);
        } catch (Exception ex) {
            Assert.fail();
        }

        int port = env.getServerPort();
        String instance = env.getInstanceName();
        String dispatcher = env.getDispatcher();
        String version = env.getDispatcherVersion();

        Assert.assertEquals(666, port);
        Assert.assertEquals("instance", instance);
        Assert.assertEquals("server", dispatcher);
        Assert.assertEquals("6.6.6", version);

        NewRelic.getAgent().getTransaction().markResponseSent();
        Transaction tx = Transaction.getTransaction(false);
        Assert.assertNotEquals(0, tx.getTransactionTimer().getResponseTimeInNanos());
    }

    /* External - FIT to Public API */

    @Test
    public void testExternalAPI() {
        try {
            runTestExternalAPI();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestExternalAPI() {
        String library = "HttpClient";
        URI uri = null;
        try {
            uri = new URI("http://localhost:8088/test/this/path?name=Bob");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String operation = "execute";

        ExternalParameters params = GenericParameters
                .library(library)
                .uri(uri)
                .procedure(operation)
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

        Tracer rootTracer = Transaction.getTransaction().getRootTracer();
        rootTracer.finish(0, null);

        Assert.assertEquals("http://localhost:8088/test/this/path", rootTracer.getTransactionSegmentUri());
        Assert.assertEquals("External/localhost/HttpClient/execute", rootTracer.getTransactionSegmentName());
        Assert.assertEquals("External/localhost/HttpClient/execute", rootTracer.getMetricName());
    }

    /* External/CAT - FIT to Public API */

    @Test
    public void testExternalCatAPI() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder(CAT_CONFIG_FILE, "cat_enabled_dt_disabled_test");
        TestServer server = new TestServer(8088);

        try {
            server.start();
            runTestExternalCatAPI();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            Transaction.clearTransaction();
            server.closeAllConnections();
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    private void runTestExternalCatAPI() {

        URL myURL = null;
        try {
            Thread.sleep(1000);
            myURL = new URL("http://localhost:8088");
            HttpUriRequest request = RequestBuilder.get().setUri(myURL.toURI()).build();

            ApiTestHelper.OutboundWrapper outboundWrapper = new ApiTestHelper.OutboundWrapper(request, HeaderType.HTTP);
            com.newrelic.api.agent.TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
            tracedMethod.addOutboundRequestHeaders(outboundWrapper);

            CloseableHttpClient connection = HttpClientBuilder.create().build();
            CloseableHttpResponse response = connection.execute(request);

            ApiTestHelper.InboundWrapper inboundHeaders = new ApiTestHelper.InboundWrapper(response, HeaderType.HTTP);
            ExternalParameters params = HttpParameters
                    .library("HttpClient")
                    .uri(myURL.toURI())
                    .procedure("execute")
                    .inboundHeaders(inboundHeaders)
                    .build();
            NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(params);

            Tracer rootTracer = Transaction.getTransaction().getRootTracer();
            rootTracer.finish(0, null);

            Map<String, Object> attributes = rootTracer.getAgentAttributes();
            Assert.assertNotNull(attributes.get("transaction_guid"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* Datastore - FIT to Public API */

    @Test
    public void testDatastoreAPI() {
        try {
            runTestDatastoreAPI();

            // test scoped Datastore metrics
            String scopedMetric = "Datastore/statement/MongoDB/Users/SELECT";
            Assert.assertEquals(1, apiTestHelper.tranStats.getScopedStats().getSize());
            Assert.assertNotNull("The following metric should exist: " + scopedMetric, apiTestHelper.tranStats.getScopedStats().getStatsMap().get(scopedMetric));

            // test unscoped Datastore metrics
            String instanceMetric = "Datastore/instance/MongoDB/awesome-host/27017";
            String operationMetric = "Datastore/operation/MongoDB/SELECT";
            Assert.assertNotNull("The following metric should exist: " + instanceMetric, apiTestHelper.tranStats.getUnscopedStats().getStatsMap().get(instanceMetric));
            Assert.assertNotNull("The following metric should exist: " + operationMetric, apiTestHelper.tranStats.getUnscopedStats().getStatsMap().get(operationMetric));
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestDatastoreAPI() {
        String vendor = "MongoDB";
        String collection = "Users";
        String operation = "SELECT";
        String host = "awesome-host";
        Integer port = 27017;

        ExternalParameters params = DatastoreParameters
                .product(vendor)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
    }

    /* Datastore/Slow Query - FIT to Public API */

    @Test
    public void testDatastoreSlowQueryAPI() {
        try {
            runTestDatastoreSlowQueryAPI();

            // test scoped Datastore metrics
            String scopedMetric = "Datastore/statement/MongoDB/Users/SELECT";
            Assert.assertEquals(1, apiTestHelper.tranStats.getScopedStats().getSize());
            Assert.assertNotNull("The following metric should exist: " + scopedMetric, apiTestHelper.tranStats.getScopedStats().getStatsMap().get(scopedMetric));

            // test unscoped Datastore metrics
            String instanceMetric = "Datastore/instance/MongoDB/awesome-host/27017";
            String operationMetric = "Datastore/operation/MongoDB/SELECT";
            Assert.assertNotNull("The following metric should exist: " + instanceMetric, apiTestHelper.tranStats.getUnscopedStats().getStatsMap().get(instanceMetric));
            Assert.assertNotNull("The following metric should exist: " + operationMetric, apiTestHelper.tranStats.getUnscopedStats().getStatsMap().get(operationMetric));
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestDatastoreSlowQueryAPI() {
        String vendor = "MongoDB";
        String collection = "Users";
        String operation = "SELECT";
        String host = "awesome-host";
        Integer port = 27017;

        BsonDocument rawQuery = new BsonDocument("key", new BsonBoolean(true));
        QueryConverter<BsonDocument> MONGO_QUERY_CONVERTER = new QueryConverter<BsonDocument>() {
            @Override
            public String toRawQueryString(BsonDocument query) {
                return query.toString();
            }

            @Override
            public String toObfuscatedQueryString(BsonDocument query) {
                return query.toString();
            }
        };

        ExternalParameters params = DatastoreParameters
                .product(vendor)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .noDatabaseName()
                .slowQuery(rawQuery, MONGO_QUERY_CONVERTER)
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Tracer rootTracer = Transaction.getTransaction(false).getRootTracer();
        rootTracer.finish(0, null);

        Map<String, Object> attributes = rootTracer.getAgentAttributes();

        Assert.assertNotNull(attributes.get("sql"));
        Assert.assertNotNull(attributes.get("sql_obfuscated"));
    }

    /* Messaging - FIT to Public API */

    @Test
    public void testMessagingAPI() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder(CAT_CONFIG_FILE, "cat_enabled_dt_disabled_test");
        MessagingTestServer server = new MessagingTestServer(8088);

        try {
            server.start();
            runTestMessagingAPI();
            String messageBrokerMetric = "MessageBroker/JMS/Queue/Consume/Temp";
            Assert.assertTrue("The following metric should exist: " + messageBrokerMetric, apiTestHelper.tranStats.getScopedStats().getStatsMap().containsKey(messageBrokerMetric));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            Transaction.clearTransaction();
            server.closeAllConnections();
            holder.close();
        }
    }


    @Test
    public void testMessagingAPIWithHostAndPort() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder(CAT_CONFIG_FILE, "cat_enabled_dt_disabled_test");
        MessagingTestServer server = new MessagingTestServer(8088);

        try {
            server.start();
            runTestMessagingAPIWithHostAndPort();
            String messageBrokerMetric = "MessageBroker/JMS/Queue/Consume/Temp";
            Assert.assertTrue("The following metric should exist: " + messageBrokerMetric, apiTestHelper.tranStats.getScopedStats().getStatsMap().containsKey(messageBrokerMetric));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            Transaction.clearTransaction();
            server.closeAllConnections();
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    private void runTestMessagingAPI() {
        URL myURL = null;
        try {
            Thread.sleep(600);
            myURL = new URL("http://localhost:8088");
            HttpUriRequest request = RequestBuilder.get().setUri(myURL.toURI()).build();

            ApiTestHelper.OutboundWrapper outboundRequestWrapper = new ApiTestHelper.OutboundWrapper(request, HeaderType.MESSAGE);

            // MessageProducer
            ExternalParameters messageProduceParameters = MessageProduceParameters
                    .library("JMS")
                    .destinationType(DestinationType.NAMED_QUEUE)
                    .destinationName("Message Destination")
                    .outboundHeaders(outboundRequestWrapper)
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

            Assert.assertTrue(request.getHeaders("NewRelicID").length != 0);
            Assert.assertTrue(request.getHeaders("NewRelicTransaction").length != 0);

            CloseableHttpClient connection = HttpClientBuilder.create().build();
            CloseableHttpResponse response = connection.execute(request);

            // MessageConsumer
            ExternalParameters messageResponseParameters = MessageConsumeParameters
                    .library("JMS")
                    .destinationType(DestinationType.TEMP_QUEUE)
                    .destinationName("Message Destination")
                    .inboundHeaders(new ApiTestHelper.InboundWrapper(response, HeaderType.MESSAGE))
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(messageResponseParameters);

            Assert.assertTrue(response.getHeaders("NewRelicAppData").length != 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Trace(dispatcher = true)
    private void runTestMessagingAPIWithHostAndPort() {
        URL myURL = null;
        try {
            Thread.sleep(600);
            myURL = new URL("http://localhost:8088");
            HttpUriRequest request = RequestBuilder.get().setUri(myURL.toURI()).build();

            ApiTestHelper.OutboundWrapper outboundRequestWrapper = new ApiTestHelper.OutboundWrapper(request, HeaderType.MESSAGE);

            // MessageProducer
            ExternalParameters messageProduceParameters = MessageProduceParameters
                    .library("JMS")
                    .destinationType(DestinationType.NAMED_QUEUE)
                    .destinationName("MessageDestination")
                    .outboundHeaders(outboundRequestWrapper)
                    .instance(myURL.getHost(), myURL.getPort())
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

            Assert.assertTrue(request.getHeaders("NewRelicID").length != 0);
            Assert.assertTrue(request.getHeaders("NewRelicTransaction").length != 0);

            CloseableHttpClient connection = HttpClientBuilder.create().build();
            CloseableHttpResponse response = connection.execute(request);

            // MessageConsumer
            ExternalParameters messageResponseParameters = MessageConsumeParameters
                    .library("JMS")
                    .destinationType(DestinationType.TEMP_QUEUE)
                    .destinationName("MessageDestination")
                    .inboundHeaders(new ApiTestHelper.InboundWrapper(response, HeaderType.MESSAGE))
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(messageResponseParameters);

            Assert.assertTrue(response.getHeaders("NewRelicAppData").length != 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testNotNull() {
        Assert.assertNotNull(NewRelic.getAgent().getTransaction().getTracedMethod());
        Assert.assertNotNull(NewRelic.getAgent().getTransaction().getToken());
        Assert.assertNotNull(NewRelic.getAgent().getTracedMethod());
    }

}
