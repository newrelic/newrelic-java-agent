/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.BoundTransactionApiImpl;
import com.newrelic.agent.HeadersUtil;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpToken;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTracePayloadParser;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceParentHeader;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.agent.util.TimeConversion;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.test.marker.RequiresFork;
import org.json.simple.JSONArray;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.objectweb.asm.Opcodes;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.newrelic.agent.AgentHelper.getFullPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Category(RequiresFork.class)
public class DefaultTracerTest {

    private String APP_NAME;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String configPath = getFullPath("/com/newrelic/agent/config/span_events.yml");
        System.setProperty("newrelic.config.file", configPath);

        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    @Before
    public void before() throws Exception {
        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        APP_NAME = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        eventPool.clear();
    }

    @Test
    public void test() {
        Transaction tx = Transaction.getTransaction();
        Assert.assertFalse(tx.isWebTransaction());
    }

    @Test
    public void testNameTx() {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer parentTracer = new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
        tx.getTransactionActivity().tracerStarted(parentTracer);
        tx.convertToWebTransaction(); //the important thing is that we're changing the dispatcher
        tx.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, "custom", "foo");
        assertEquals(0, AgentHelper.getChildren(parentTracer).size());
        parentTracer.finish(Opcodes.RETURN, null);
        assertEquals("WebTransaction/custom/foo", tx.getPriorityTransactionName().getName());
        assertClmAbsent(parentTracer);
    }

    @Test
    public void sizeofString() {
        assertEquals(9, DefaultTracer.sizeof("Dude man!"));
    }

    @Test
    public void sizeofArray() {
        assertEquals(7, DefaultTracer.sizeof(new String[] { "Dude", "man" }));
    }

    @Test
    public void sizeofStackTrace() {
        StackTraceElement stackTraceElement = new StackTraceElement("foo.Bar", "getFoo", "foo/Bar.java", 100);
        StackTraceElement[] stack = new StackTraceElement[] { stackTraceElement,
                new StackTraceElement("foo.Bar", "getBar", "foo/Bar.java", 120) };
        assertEquals(35, DefaultTracer.sizeof(stackTraceElement));
        assertEquals(70, DefaultTracer.sizeof(stack));
    }

    @Test
    public void exclusiveTime() throws Exception {
        Transaction tx = Transaction.getTransaction();

        long parentDuration = 100;
        long childDuration = 100;
        int childCount = 3;

        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer parentTracer = new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
        tx.getTransactionActivity().tracerStarted(parentTracer);
        assertEquals(0, AgentHelper.getChildren(parentTracer).size());

        long actualChildDuratioNanos = 0;
        synchronized (this) {
            wait(parentDuration);

            for (int i = 0; i < childCount; i++) {
                DefaultTracer kid = new DefaultTracer(tx, sig, this);
                tx.getTransactionActivity().tracerStarted(kid);
                wait(childDuration);
                kid.finish(Opcodes.RETURN, null);
                // parentTracer.childTracerFinished(kid);
                actualChildDuratioNanos += kid.getDuration();
            }
        }

        parentTracer.finish(Opcodes.RETURN, null);

        Assert.assertTrue(parentTracer.getDurationInMilliseconds() >= parentDuration);
        Assert.assertNotSame(parentTracer.getExclusiveDuration(), parentTracer.getDuration());
        assertEquals(actualChildDuratioNanos, parentTracer.getDuration() - parentTracer.getExclusiveDuration());

        assertEquals(childCount, AgentHelper.getChildren(parentTracer).size());
        assertClmAbsent(parentTracer);
    }

    @Test
    public void testParameters() {
        try {
            DefaultTracer kid = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature(
                    getClass().getName(), "dude", "()V"), this);
            Assert.assertNotNull(kid.getAgentAttributes());
            Assert.assertTrue(kid.getAgentAttributes().isEmpty());

            kid.setAgentAttribute("key", "value");
            assertEquals("value", kid.getAgentAttribute("key"));
            assertNull(kid.getAgentAttribute("notpresent"));
            assertClmAbsent(kid);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Test
    public void stackTrace() throws Exception {
        Transaction transaction = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("java.lang.String", "valueof", "(J)Ljava/lang/String;");
        DefaultTracer tracer = new DefaultTracer(transaction, sig, "12345");
        tracer.storeStackTrace();

        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
        TransactionTracerConfig ttConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig();
        TransactionSegment segment = new TransactionSegment(ttConfig, sqlObfuscator, 0, tracer);
        JSONArray json = (JSONArray) AgentHelper.serializeJSON(segment);

        Map params = (Map) json.get(3);
        List backtrace = (List) params.get(DefaultSqlTracer.BACKTRACE_PARAMETER_NAME);

        Assert.assertTrue(backtrace.size() > 2);
        assertClmAbsent(tracer);
    }

    @Test
    public void testExternalParameters() throws URISyntaxException {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        TransactionActivity txa = TransactionActivity.get();
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.TracedActivityTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);
        final TransactionStats stats = root.getTransactionActivity().getTransactionStats();

        // http external
        final String library = "unittest";
        final URI uri = new URI("http://localhost");
        final String host = uri.getHost();
        final String procedure = "connect";
        int externalCount = 0;
        { // generic external
            DefaultTracer tracer = (DefaultTracer) AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                    DefaultTracer.DEFAULT_TRACER_FLAGS);

            assertExternal(stats, externalCount, host, library, procedure);

            // multiple calls to addExternalParameters should only apply the last call
            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());
            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());
            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());

            // metrics should only be recorded when the tracer finishes
            assertExternal(stats, externalCount, host, library, procedure);
            assertEquals(externalCount, stats.getScopedStats().getOrCreateResponseTimeStats("External/" + host + "/"
                    + library + "/" + procedure).getCallCount());
            tracer.finish(0, null);
            externalCount++;
            assertExternal(stats, externalCount, host, library, procedure);
            assertEquals(externalCount, stats.getScopedStats().getOrCreateResponseTimeStats("External/" + host + "/"
                    + library + "/" + procedure).getCallCount());
            assertClmAbsent(tracer);
        }

        final DatastoreVendor vendor = DatastoreVendor.MySQL;
        final String collection = "stores";
        final String operation = "select";
        final int port = 666;
        int datastoreCount = 0;
        { // datastore
            DefaultTracer tracer = (DefaultTracer) AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                    DefaultTracer.DEFAULT_TRACER_FLAGS);

            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());
            tracer.reportAsExternal(DatastoreParameters
                    .product(vendor.toString())
                    .collection(collection)
                    .operation(operation)
                    .instance(host, port)
                    .build());
            assertDatastore(stats, datastoreCount, vendor.toString(), collection, operation, host, port);
            tracer.finish(0, null);
            datastoreCount++;
            assertDatastore(stats, datastoreCount, vendor.toString(), collection, operation, host, port);
            // http external should be unchanged
            assertExternal(stats, externalCount, host, library, procedure);
            assertClmAbsent(tracer);
        }

        { // http + DT
            DefaultTracer tracer = (DefaultTracer) AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                    DefaultTracer.DEFAULT_TRACER_FLAGS);

            tracer.addOutboundRequestHeaders(new Outbound());
            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());
            tracer.reportAsExternal(HttpParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .inboundHeaders(new Inbound("Foo"))
                    .build());
            assertCat(tracer, false);
            assertExternal(stats, externalCount, host, library, procedure);
            tracer.finish(0, null);
            externalCount++;
            assertCat(tracer, false); // DT is enabled, there should not be any CAT
            // ExternalTransaction/localhost/12345/Foo
            assertExternal(stats, externalCount, host, library, procedure);
            assertEquals(0, stats.getScopedStats().getOrCreateResponseTimeStats("ExternalTransaction/" + host
                    + "/12345/Foo").getCallCount());
            assertClmAbsent(tracer);
        }

        { // last inboundHeaders win
            DefaultTracer tracer = (DefaultTracer) AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                    DefaultTracer.DEFAULT_TRACER_FLAGS);

            tracer.addOutboundRequestHeaders(new Outbound());
            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());
            tracer.reportAsExternal(HttpParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .inboundHeaders(new Inbound("Foo"))
                    .build());
            tracer.readInboundResponseHeaders(new Inbound("Bar")); // headers trump the previous call
            assertCat(tracer, false);
            assertExternal(stats, externalCount, host, library, procedure);
            tracer.finish(0, null);
            externalCount++;
            assertCat(tracer, false); // DT is enabled, there should not be any CAT
            // ExternalTransaction/localhost/12345/Foo
            assertExternal(stats, externalCount, host, library, procedure);
            assertEquals(0, stats.getScopedStats().getOrCreateResponseTimeStats("ExternalTransaction/" + host
                    + "/12345/Bar").getCallCount());
            assertClmAbsent(tracer);
        }

        { // set headers manually
            DefaultTracer tracer = (DefaultTracer) AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                    DefaultTracer.DEFAULT_TRACER_FLAGS);

            tracer.addOutboundRequestHeaders(new Outbound());
            tracer.reportAsExternal(GenericParameters
                    .library(library)
                    .uri(uri)
                    .procedure(procedure)
                    .build());
            tracer.readInboundResponseHeaders(new Inbound("Baz")); // headers trump the previous call
            assertCat(tracer, false);
            assertExternal(stats, externalCount, host, library, procedure);
            tracer.finish(0, null);
            externalCount++;
            assertCat(tracer, false); // DT is enabled, there should not be any CAT
            // ExternalTransaction/localhost/12345/Foo
            assertExternal(stats, externalCount, host, library, procedure);
            assertEquals(0, stats.getScopedStats().getOrCreateResponseTimeStats("ExternalTransaction/" + host
                    + "/12345/Baz").getCallCount());
            assertClmAbsent(tracer);
        }

        root.finish(0, null);
    }

    @Test
    public void testTokenRefNoOpToken() throws URISyntaxException {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        TransactionActivity txa = TransactionActivity.get();
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.TracedActivityTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);

        final Token token = NoOpToken.INSTANCE;

        TransactionActivity.clear();
        Transaction.clearTransaction();

        final AgentBridge.TokenAndRefCount tokenAndRefCount = new AgentBridge.TokenAndRefCount(token, root, new AtomicInteger(1));
        AgentBridge.activeToken.set(tokenAndRefCount);

        DefaultTracer tracer = (DefaultTracer) AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                DefaultTracer.DEFAULT_TRACER_FLAGS);

        assertNull(tracer);

        root.finish(0, null);
        assertClmAbsent(root);
    }

    @Test
    public void testTokenRefToken() throws URISyntaxException {
        testTokenRefRoken(false);
    }

    @Test
    public void testTokenRefTokenSql() throws URISyntaxException {
        testTokenRefRoken(true);
    }

    private void testTokenRefRoken(boolean isSqlTracer) throws URISyntaxException {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        TransactionActivity txa = TransactionActivity.get();
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.TracedActivityTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);

        final Token token = tx.getToken();

        TransactionActivity.clear();
        Transaction.clearTransaction();

        final AgentBridge.TokenAndRefCount tokenAndRefCount = new AgentBridge.TokenAndRefCount(token, root, new AtomicInteger(1));
        AgentBridge.activeToken.set(tokenAndRefCount);

        DefaultTracer tracer = (DefaultTracer)
                (isSqlTracer ?
                    AgentBridge.instrumentation.createSqlTracer(null, 0,
                            "iamyourchild", DefaultTracer.DEFAULT_TRACER_FLAGS) :
                    AgentBridge.instrumentation.createTracer(null, 0,
                            "iamyourchild", DefaultTracer.DEFAULT_TRACER_FLAGS));

        Assert.assertNotNull(tracer);

        root.finish(0, null);
        assertClmAbsent(root);
        assertClmAbsent(tracer);
    }

    @Test
    public void testCreateTracerNoToken() {
        testCreateTracerNoToken(false);
    }

    @Test
    public void testCreateSqlTracerNoToken() {
        testCreateTracerNoToken(true);
    }

    public void testCreateTracerNoToken(boolean isSqlTracer) {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        TransactionActivity txa = TransactionActivity.get();
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.TracedActivityTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);

        TransactionActivity.clear();
        Transaction.clearTransaction();

        DefaultTracer tracer = (DefaultTracer)
                (isSqlTracer ?
                        AgentBridge.instrumentation.createSqlTracer(null, 0,
                                "iamyourchild", DefaultTracer.DEFAULT_TRACER_FLAGS | TracerFlags.DISPATCHER) :
                        AgentBridge.instrumentation.createTracer(null, 0,
                                "iamyourchild", DefaultTracer.DEFAULT_TRACER_FLAGS | TracerFlags.DISPATCHER));

        Assert.assertNotNull(tracer);

        root.finish(0, null);
        assertClmAbsent(root);
        assertClmAbsent(tracer);
    }

    @Test
    public void testExternalParametersNullHost() {
        DefaultTracer tracer = prepareTracer();
        TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();

        URI uriWithNoHost = URI.create("../icons/logo.gif");
        tracer.reportAsExternal(HttpParameters
                .library("library")
                .uri(uriWithNoHost)
                .procedure("procedure")
                .noInboundHeaders()
                .build());
        tracer.recordMetrics(stats);

        // this test lacked assertions prior to this one
        assertClmAbsent(tracer);
    }

    @Test
    public void testGenericParametersNoHost() {
        DefaultTracer tracer = prepareTracer();
        TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();

        URI uriWithNoHost = URI.create("../icons/logo.gif");
        tracer.reportAsExternal(HttpParameters
                .library("library")
                .uri(uriWithNoHost)
                .procedure("procedure")
                .noInboundHeaders()
                .build());
        tracer.recordMetrics(stats);

        // this test lacked assertions prior to this one
        assertClmAbsent(tracer);
    }

    @Test
    public void testDatastoreParametersNoHost() {
        DefaultTracer tracer = prepareTracer();
        TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();

        tracer.reportAsExternal(DatastoreParameters
                .product("Product")
                .collection("collection")
                .operation("operation")
                .instance(null, 91929)
                .build());
        tracer.recordMetrics(stats);
        checkUnknownDatastoreSupportabilityMetrics("Product", 1, 0, 1);
        assertClmAbsent(tracer);
    }

    @Test
    public void testNoParametersInUri() {
        DefaultTracer tracer = prepareTracer();
        TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();
        String uri = "http://myhost:1234/Parameters";
        String queryParams = "?data=confidential";
        tracer.reportAsExternal(GenericParameters
                .library("MyLibrary")
                .uri(URI.create(uri + queryParams))
                .procedure("other")
                .build());
        tracer.recordMetrics(stats);
        assertEquals(uri, tracer.getTransactionSegmentUri());
        assertClmAbsent(tracer);
    }

    @Test
    public void testInstanceLevelMetrics() {
        checkInstanceMetric("DB", "myfavorite.host", 12345, "myfavorite.host", "12345");
        checkInstanceMetric("DB", "myfavorite.host", null, "myfavorite.host", "unknown");
        checkUnknownDatastoreSupportabilityMetrics("DB", 0, 1, 2);
    }

    @Test
    public void testInstanceLevelMetricLocalhost() throws UnknownHostException {
        final String hostname = InetAddress.getLocalHost().getHostName();

        checkInstanceMetric("DB", "localhost", 3360, hostname, "3360");
        checkInstanceMetric("DB", "127.0.0.1", 3360, hostname, "3360");
        checkInstanceMetric("DB", "0.0.0.0", 3360, hostname, "3360");
        checkInstanceMetric("DB", "0:0:0:0:0:0:0:1", 3360, hostname, "3360");
        checkInstanceMetric("DB", "::1", 3360, hostname, "3360");
        checkInstanceMetric("DB", "0:0:0:0:0:0:0:0", 3360, hostname, "3360");
        checkInstanceMetric("DB", "::", 3360, hostname, "3360");
        checkUnknownDatastoreSupportabilityMetrics("DB", 0, 0, 7);
    }

    @Test
    public void testAllSupportabilityMetrics() {
        DefaultTracer tracer = prepareTracer();

        String unknownPort = null;
        String unknownHost = null;

        String product = "Product";
        ExternalParameters parameters = DatastoreParameters
                .product(product)
                .collection("Collection")
                .operation("operation")
                .instance(unknownHost, unknownPort)
                .noDatabaseName()
                .build();

        tracer.reportAsExternal(parameters);
        tracer.finish(0, null);

        checkUnknownDatastoreSupportabilityMetrics("Product", 1, 1, 1);
        assertClmAbsent(tracer);
    }

    @Test
    public void testNoPortSupportabilityMetrics() {
        DefaultTracer tracer = prepareTracer();

        String product = "Product";
        ExternalParameters parameters = DatastoreParameters
                .product(product)
                .collection("Collection")
                .operation("operation")
                .instance("myHost", 12345)
                .databaseName("databaseName")
                .build();

        tracer.reportAsExternal(parameters);
        tracer.finish(0, null);
        checkUnknownDatastoreSupportabilityMetrics("Product", 0, 0, 0);
        assertClmAbsent(tracer);
    }

    @Test
    public void testInstanceButNoPortSupportabilityMetrics() {
        DefaultTracer tracer = prepareTracer();

        String product = "Product";
        ExternalParameters parameters = DatastoreParameters
                .product(product)
                .collection("Collection")
                .operation("operation")
                .instance("myHost", "instanceId")
                .databaseName("databaseName")
                .build();

        tracer.reportAsExternal(parameters);
        tracer.finish(0, null);
        checkUnknownDatastoreSupportabilityMetrics("Product", 0, 0, 0);
        assertClmAbsent(tracer);
    }

    @Test
    public void testSpanEvent() {
        DefaultTracer tracer = prepareTracer();
        tracer.finish(0, null);

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        assertNull(spanEvent.getParentId());
        assertEquals(tracer.getGuid(), spanEvent.getGuid());
        assertEquals(tracer.getTransaction().getGuid(), spanEvent.getTransactionId());
        assertEquals("Java/class/method", spanEvent.getName());
        assertEquals(true, spanEvent.getIntrinsics().get("nr.entryPoint"));
        assertEquals((float) tracer.getDurationInMilliseconds() / TimeConversion.MILLISECONDS_PER_SECOND,
                spanEvent.getDuration(), 0.001f);
        assertClmAbsent(spanEvent);
    }

    @Test
    public void testSpanEventHttp() {
        DefaultTracer tracer = prepareTracer();
        tracer.reportAsExternal(HttpParameters.library("library")
                .uri(URI.create("http://www.newrelic.com"))
                .procedure("call")
                .noInboundHeaders().build());
        tracer.finish(0, null);

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        assertNull(spanEvent.getParentId());
        assertEquals("library", spanEvent.getIntrinsics().get("component"));
        assertEquals("client", spanEvent.getIntrinsics().get("span.kind"));
        assertEquals("http://www.newrelic.com", spanEvent.getAgentAttributes().get("http.url"));
        assertEquals("call", spanEvent.getAgentAttributes().get("http.method"));
        assertClmAbsent(spanEvent);
    }

    @Test
    public void testSpanEventDatastore() {
        DefaultTracer tracer = prepareTracer();
        tracer.reportAsExternal(DatastoreParameters.product("YourSQL")
                .collection("collection")
                .operation("query")
                .instance("databaseServer", 1234)
                .databaseName("dbName")
                .cloudResourceId("cloudResourceId")
                .build());
        tracer.finish(0, null);

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        assertNull(spanEvent.getParentId());
        assertEquals("YourSQL", spanEvent.getAgentAttributes().get("db.system"));
        assertEquals("databaseServer", spanEvent.getAgentAttributes().get("peer.hostname"));
        assertEquals("dbName", spanEvent.getAgentAttributes().get("db.instance"));
        assertEquals("databaseServer:1234", spanEvent.getAgentAttributes().get("peer.address"));
        assertEquals("client", spanEvent.getIntrinsics().get("span.kind"));
        assertEquals("cloudResourceId", spanEvent.getAgentAttributes().get("cloud.resource_id"));
        assertClmAbsent(spanEvent);
    }

    @Test
    public void testMultiTransactionSpanEvents() {
        DistributedTraceServiceImpl dts = (DistributedTraceServiceImpl) ServiceFactory.getServiceManager().getDistributedTraceService();

        Map<String, Object> configMap = ImmutableMap.<String, Object>builder().put("cross_application_tracer",
                ImmutableMap.builder().put("cross_process_id", "12345#whatever")
                        .put("trusted_account_key", "67890").build())
                .build();
        dts.connected(null, AgentConfigFactory.createAgentConfig(configMap, null, null));

        DefaultTracer firstTracer = prepareTracer();
        BoundTransactionApiImpl firstTxn = new BoundTransactionApiImpl(firstTracer.getTransaction());
        DistributedTracePayload payload = firstTxn.createDistributedTracePayload();
        firstTracer.finish(0, null);

        Transaction.clearTransaction();

        DefaultTracer secondTracer = prepareTracer();
        BoundTransactionApiImpl secondTxn = new BoundTransactionApiImpl(secondTracer.getTransaction());
        secondTxn.acceptDistributedTracePayload(payload);
        DistributedTracePayload secondPayload = secondTxn.createDistributedTracePayload();
        secondTracer.finish(0, null);

        Transaction.clearTransaction();

        DefaultTracer thirdTracer = prepareTracer();
        BoundTransactionApiImpl thirdTxn = new BoundTransactionApiImpl(thirdTracer.getTransaction());
        thirdTxn.acceptDistributedTracePayload(secondPayload);
        DistributedTracePayload thirdPayload = thirdTxn.createDistributedTracePayload();
        thirdTracer.finish(0, null);

        Transaction.clearTransaction();
        DefaultTracer fourthTracer = prepareTracer();
        BoundTransactionApiImpl fourthTxn = new BoundTransactionApiImpl(fourthTracer.getTransaction());
        fourthTxn.acceptDistributedTracePayload(thirdPayload);
        fourthTracer.finish(0, null);

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(firstTracer.getTransaction(), 1024), new TransactionStats());
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(secondTracer.getTransaction(), 1024), new TransactionStats());
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(thirdTracer.getTransaction(), 1024), new TransactionStats());
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(fourthTracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(4, spanEvents.size());

        // Make sure traceIds of all span events matches trace id of first payload sent
        DistributedTracePayloadImpl parsedPayload = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(),
                ServiceFactory.getDistributedTraceService(),
                Agent.LOG).parse(null, payload.text());
        assertNotNull(parsedPayload);
        for (SpanEvent event : spanEvents) {
            assertEquals("Span events must have same trace id", parsedPayload.traceId, event.getTraceId());
            assertClmAbsent(event);
        }
    }

    @Test
    public void testParent() {
        DefaultTracer tracer = prepareTracer();

        Tracer child = new DefaultTracer(tracer.getTransaction(), new ClassMethodSignature("com.package.modern.Customer",
                "child", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tracer.getTransactionActivity().tracerStarted(child);

        Tracer child2 = new DefaultTracer(tracer.getTransaction(), new ClassMethodSignature("com.package.modern.Customer",
                "child2", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        child.getTransactionActivity().tracerStarted(child2);

        child2.reportAsExternal(DatastoreParameters.product("YourSQL")
                .collection("collection")
                .operation("query")
                .instance("databaseServer", 1234)
                .databaseName("dbName")
                .build());
        child2.finish(0, null);

        child.finish(0, null);

        Tracer sibiling = new DefaultTracer(tracer.getTransaction(), new ClassMethodSignature("com.package.modern.Customer",
                "sibiling", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tracer.getTransactionActivity().tracerStarted(sibiling);

        sibiling.reportAsExternal(HttpParameters.library("library")
                .uri(URI.create("https://myservice:8080/api"))
                .procedure("call")
                .noInboundHeaders().build());
        sibiling.finish(0, null);
        tracer.finish(0, null);

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(4, spanEvents.size());

        SpanEvent rootSpan = null;
        SpanEvent childSpan = null;
        SpanEvent child2Span = null;
        SpanEvent siblingSpan = null;

        for (SpanEvent spanEvent : spanEvents) {
            if (spanEvent.getGuid().equals(tracer.getGuid())) {
                rootSpan = spanEvent;
            } else if (spanEvent.getGuid().equals(child.getGuid())) {
                childSpan = spanEvent;
            } else if (spanEvent.getGuid().equals(child2.getGuid())) {
                child2Span = spanEvent;
            } else if (spanEvent.getGuid().equals(sibiling.getGuid())) {
                siblingSpan = spanEvent;
            }
        }

        assertNotNull(rootSpan);
        assertNotNull(childSpan);
        assertNotNull(child2Span);
        assertNotNull(siblingSpan);

        assertEquals(tracer.getTransaction().getGuid(), rootSpan.getTransactionId());
        assertEquals(tracer.getTransaction().getGuid(), childSpan.getTransactionId());
        assertEquals(tracer.getTransaction().getGuid(), child2Span.getTransactionId());
        assertEquals(tracer.getTransaction().getGuid(), siblingSpan.getTransactionId());

        assertNull(rootSpan.getParentId());
        assertEquals(true, rootSpan.getIntrinsics().get("nr.entryPoint"));
        assertEquals(rootSpan.getGuid(), childSpan.getParentId());
        assertEquals(child.getGuid(), child2Span.getParentId());
        assertNull(child2Span.getIntrinsics().get("nr.entryPoint"));
        assertEquals(rootSpan.getGuid(), siblingSpan.getParentId());
        assertNull(siblingSpan.getIntrinsics().get("nr.entryPoint"));

        assertEquals("YourSQL", child2Span.getAgentAttributes().get("db.system"));
        assertEquals("databaseServer", child2Span.getAgentAttributes().get("peer.hostname"));
        assertEquals("dbName", child2Span.getAgentAttributes().get("db.instance"));
        assertEquals("databaseServer:1234", child2Span.getAgentAttributes().get("peer.address"));
        assertEquals("client", child2Span.getIntrinsics().get("span.kind"));

        assertEquals("library", siblingSpan.getIntrinsics().get("component"));
        assertEquals("client", siblingSpan.getIntrinsics().get("span.kind"));
        assertEquals("call", siblingSpan.getAgentAttributes().get("http.method"));
        assertEquals("https://myservice:8080/api", siblingSpan.getAgentAttributes().get("http.url"));
    }

    /**
     * Transaction A has spans 1, 2, and 3. Span 2 is active when a distributed tracing payload is created.
     * Transaction B has spans 5 and 6. The payload from span 2 is accepted when span 5 is active.
     *
     * Verify that all the parenting attributes are correct.
     */
    @Test
    public void testTransactionABParenting() {
        DistributedTraceServiceImpl dts = (DistributedTraceServiceImpl) ServiceFactory.getServiceManager().getDistributedTraceService();
        Map<String, Object> configMap = ImmutableMap.<String, Object>builder().put("distributed_tracing",
                ImmutableMap.builder()
                        .put("account_id", "12345")
                        .put("trusted_account_key", "67890")
                        .put("primary_application_id", "789").build()).build();
        dts.connected(null, AgentConfigFactory.createAgentConfig(configMap, null, null));

        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();

        DefaultTracer span1Tracer = new OtherRootTracer(tx, new ClassMethodSignature("class",
                "span1", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tx.getTransactionActivity().tracerStarted(span1Tracer);

        tx.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, true, "Transaction A");

        DefaultTracer span2Tracer = new OtherRootTracer(tx, new ClassMethodSignature("class",
                "span2", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tx.getTransactionActivity().tracerStarted(span2Tracer);

        DistributedTracePayload payload = new BoundTransactionApiImpl(span2Tracer.getTransaction()).createDistributedTracePayload();

        DefaultTracer span3Tracer = new OtherRootTracer(tx, new ClassMethodSignature("class",
                "span3", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tx.getTransactionActivity().tracerStarted(span3Tracer);

        span3Tracer.finish(Opcodes.ARETURN, null);
        span2Tracer.finish(Opcodes.ARETURN, null);
        span1Tracer.finish(Opcodes.ARETURN, null);

        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction txB = Transaction.getTransaction();

        DefaultTracer span5Tracer = new OtherRootTracer(txB, new ClassMethodSignature("class",
                "span5", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txB.getTransactionActivity().tracerStarted(span5Tracer);
        span5Tracer.getTransaction().acceptDistributedTracePayload(payload, null);

        txB.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, true, "Transaction B");
        txB.setThrowable(new Throwable(), TransactionErrorPriority.API, false);

        DefaultTracer span6Tracer = new OtherRootTracer(txB, new ClassMethodSignature("class",
                "span6", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txB.getTransactionActivity().tracerStarted(span6Tracer);

        span6Tracer.finish(Opcodes.ARETURN, null);
        span5Tracer.finish(Opcodes.ARETURN, null);

        // assert traceId
        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        final TransactionData tdA = new TransactionData(tx, 1024);
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(tdA, new TransactionStats());
        final TransactionData tdB = new TransactionData(txB, 1024);
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(tdB, new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertEquals(5, spanEvents.size());

        TransactionEvent txAEvent = ServiceFactory.getTransactionEventsService().createEvent(tdA, new TransactionStats(), tdA.getBlameMetricName());
        TransactionEvent txBEvent = ServiceFactory.getTransactionEventsService().createEvent(tdB, new TransactionStats(), tdB.getBlameMetricName());

        SpanEvent span1 = getSpanByName(eventPool, "Java/class/span1");
        SpanEvent span2 = getSpanByName(eventPool, "Java/class/span2");
        SpanEvent span3 = getSpanByName(eventPool, "Java/class/span3");
        SpanEvent span5 = getSpanByName(eventPool, "Java/class/span5");
        SpanEvent span6 = getSpanByName(eventPool, "Java/class/span6");

        DistributedTracePayloadImpl parsedPayload = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(),
                ServiceFactory.getDistributedTraceService(),
                Agent.LOG).parse(null, payload.text());
        assertEquals(tx.getGuid(), parsedPayload.txnId);
        assertEquals(tx.sampled(), parsedPayload.sampled.booleanValue());
        assertEquals(tx.getPriority(), parsedPayload.priority, 0.0f);
        assertEquals(span2Tracer.getGuid(), parsedPayload.guid);
        assertEquals(span2.getGuid(), parsedPayload.guid);
        assertEquals(txAEvent.getTripId(), parsedPayload.traceId);
        assertEquals(txBEvent.getTripId(), parsedPayload.traceId);

        assertEquals(span1.getTraceId(), span2.getTraceId());
        assertEquals(span2.getTraceId(), span3.getTraceId());
        assertEquals(span3.getTraceId(), span5.getTraceId());
        assertEquals(span5.getTraceId(), span6.getTraceId());

        assertEquals(txAEvent.getGuid(), span1.getTransactionId());

        assertEquals(span1.getGuid(), span2.getParentId());
        assertEquals(txAEvent.getGuid(), span2.getTransactionId());

        assertEquals(span2.getGuid(), span3.getParentId());
        assertEquals(txAEvent.getGuid(), span3.getTransactionId());

        assertEquals(txAEvent.getGuid(), txBEvent.getParentId());
        assertEquals(span2.getGuid(), txBEvent.getParenSpanId());

        assertEquals(span2.getGuid(), span5.getParentId());
        assertEquals(txBEvent.getGuid(), span5.getTransactionId());

        assertEquals(span5.getGuid(), span6.getParentId());
        assertEquals(txBEvent.getGuid(), span6.getTransactionId());

        ServiceFactory.getTransactionService().transactionFinished(tdB, new TransactionStats());
        ErrorServiceImpl errorService = (ErrorServiceImpl) txB.getRPMService().getErrorService();
        DistributedSamplingPriorityQueue<ErrorEvent> reservoir = errorService.getReservoir(
                ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName());
        List<ErrorEvent> errorEvents = reservoir.asList();
        assertEquals(1, errorEvents.size());
        ErrorEvent errorEvent = errorEvents.get(0);
        Map<String, Object> errorDtAtts = errorEvent.getDistributedTraceIntrinsics();
        assertEquals(txBEvent.getTripId(), errorDtAtts.get("traceId"));
        assertEquals(txBEvent.getGuid(), errorEvent.getTransactionGuid());
    }

    @Test
    public void testSpanParentingEvent() {
        DefaultTracer tracer = prepareTracer();
        tracer.finish(0, null);

        SpanProxy spanProxy = tracer.getTransaction().getSpanProxy();
        W3CTraceParentHeader.create(spanProxy, "12341234123412341234123412341234", "0101010101010101", false);

        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        ((SpanEventsServiceImpl) spanEventService).dispatcherTransactionFinished(new TransactionData(tracer.getTransaction(), 1024), new TransactionStats());

        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        List<SpanEvent> spanEvents = eventPool.asList();
        assertNotNull(spanEvents);
        assertEquals(1, spanEvents.size());

        SpanEvent spanEvent = Iterables.getFirst(spanEvents, null);
        assertNotNull(spanEvent);

        Assert.assertNotEquals("0101010101010101", spanEvent.getParentId());
    }

    private SpanEvent getSpanByName(SamplingPriorityQueue<SpanEvent> eventPool, String spanName) {
        for (SpanEvent spanEvent : eventPool.asList()) {
            if (spanEvent.getName().equals(spanName)) {
                return spanEvent;
            }
        }
        return null;
    }

    private void sleep(int ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() <= end) {
        }
    }

    /**
     * @param host Host to report.
     * @param port Port to report.
     * @param expectedHost expected host in instance metric.
     * @param expectedInstanceID expected identifier in instance metric.
     */
    public void checkInstanceMetric(String product, String host, Integer port, String expectedHost, String expectedInstanceID) {
        DefaultTracer tracer = prepareTracer();

        final TransactionStats stats = tracer.getTransactionActivity().getTransactionStats();
        tracer.reportAsExternal(DatastoreParameters
                .product(product)
                .collection("MyCollection")
                .operation("operation")
                .instance(host, port)
                .build());
        tracer.finish(0, null);
        ResponseTimeStats responseTimeStats = stats.getUnscopedStats().getOrCreateResponseTimeStats(
                String.format("Datastore/instance/DB/%s/%s", expectedHost, expectedInstanceID));

        assertEquals(responseTimeStats.getCallCount(), 1);
        assertClmAbsent(tracer);
    }

    private void checkUnknownDatastoreSupportabilityMetrics(String product, int expectedUnkownHost, int expectedUnknownPort,
            int expectedUnknownDatabaseName) {
        StatsService statsService = ServiceFactory.getStatsService();
        StatsEngine statsEngine = statsService.getStatsEngineForHarvest("Unit Test");

        String unknownHostMetricName = new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                .append(product).append(MetricNames.SUPPORTABILITY_DATASTORE_UNKNOWN_HOST).toString();
        Stats unknownHostMetric = statsEngine.getStats(unknownHostMetricName);
        assertEquals(expectedUnkownHost, unknownHostMetric.getCallCount());

        String unknownPortMetricName = new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                .append(product).append(MetricNames.SUPPORTABILITY_DATASTORE_UNKNOWN_PORT).toString();
        Stats unknownPortMetric = statsEngine.getStats(unknownPortMetricName);
        assertEquals(expectedUnknownPort, unknownPortMetric.getCallCount());

        String unknownDatabaseMetricName = new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                .append(product).append(MetricNames.SUPPORTABILITY_DATASTORE_UNKNOWN_DATABASE_NAME).toString();
        Stats unknownDatabaseNameMetric = statsEngine.getStats(unknownDatabaseMetricName);
        assertEquals(expectedUnknownDatabaseName, unknownDatabaseNameMetric.getCallCount());
    }

    public DefaultTracer prepareTracer() {
        TransactionActivity.clear();
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();

        DefaultTracer tracer = new OtherRootTracer(tx, new ClassMethodSignature("class", "method", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        tx.getTransactionActivity().tracerStarted(tracer);

        return tracer;
    }

    private static void assertClmAbsent(Tracer tracer) {
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_NAMESPACE));
        assertNull(tracer.getAgentAttribute(AttributeNames.CLM_FUNCTION));
    }

    private static void assertClmAbsent(SpanEvent spanEvent) {
        Map<String, Object> agentAttributes = spanEvent.getAgentAttributes();
        assertNull(agentAttributes.get(AttributeNames.CLM_NAMESPACE));
        assertNull(agentAttributes.get(AttributeNames.CLM_FUNCTION));
    }

    private static void assertDatastore(TransactionStats stats, int count, String vendor, String collection,
            String operation, String host, int port) {
        // unscoped
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").getCallCount());
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allOther").getCallCount());
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/" + vendor
                + "/allOther").getCallCount());
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/operation/" + vendor + "/"
                + operation).getCallCount());
        // scoped
        assertEquals(count, stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/" + vendor + "/"
                + collection + "/" + operation).getCallCount());
    }

    private static void assertCat(DefaultTracer tracer, boolean catExpected) {
        if (catExpected) {
            Assert.assertNotNull(tracer.getAgentAttribute(AttributeNames.TRANSACTION_TRACE_ID_PARAMETER_NAME));
        } else {
            assertNull(tracer.getAgentAttribute(AttributeNames.TRANSACTION_TRACE_ID_PARAMETER_NAME));
        }
    }

    private static void assertExternal(TransactionStats stats, int count, String host, String library,
            String procedure) {
        // unscoped
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").getCallCount());
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allOther").getCallCount());
        assertEquals(count, stats.getUnscopedStats().getOrCreateResponseTimeStats("External/" + host + "/all").getCallCount());
        // scoped
    }

    private static class Outbound implements OutboundHeaders {
        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public void setHeader(String name, String value) {
        }
    }

    private static class Inbound extends ExtendedInboundHeaders {
        final String txName;

        public Inbound(String txName) {
            this.txName = txName;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            if (HeadersUtil.NEWRELIC_APP_DATA_HEADER.equals(name)) {
                return Obfuscator.obfuscateNameUsingKey("[\"12345\",\"" + txName + "\",0.0,1.0,-1,\"fjdfjf\"]",
                        Transaction.getTransaction().getCrossProcessConfig().getEncodingKey());
            }
            return "mock";
        }
    }

}
