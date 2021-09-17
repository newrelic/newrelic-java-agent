/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.HeadersUtil;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.NoOpSegment;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExternalAsyncTest extends AsyncTest {
    private static final String ASYNC_TXA_NAME = "activity";

    private static final String HOST = "www.example.com";
    private static final String LIBRARY = "library";
    private static final String URI = "http://www.example.com/some/path";
    private static final String OPERATION_NAME = "operation";
    private static final String CONFIG_FILE = "configs/cross_app_tracing_test.yml";
    private static final ClassLoader CLASS_LOADER = ExternalAsyncTest.class.getClassLoader();
    private ExecutorService executorService;

    private static Segment startSegment(
            OutboundHeaders outbound) {
        Segment externalEvent = NewRelic.getAgent().getTransaction().startSegment(ASYNC_TXA_NAME);
        Assert.assertNotNull(externalEvent);
        externalEvent.addOutboundRequestHeaders(outbound);
        return externalEvent;
    }

    private static void finishExternalEvent(Segment externalEvent,
            Throwable t, InboundHeaders inbound, String host, String library,
            String uri, String operationName) {
        try {
            externalEvent.reportAsExternal(HttpParameters
                    .library(library)
                    .uri(new java.net.URI(uri))
                    .procedure(operationName)
                    .inboundHeaders(inbound)
                    .build());
        } catch (URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
        externalEvent.end();
    }

    public EnvironmentHolder setupEnvironmentHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Before
    public void before() {
        executorService = Executors.newCachedThreadPool();
    }

    @After
    public void after() {
        executorService.shutdownNow();
        executorService = null;
    }

    @Test
    public void testSuccess() throws Exception {
        long time = doInTransaction((Callable<Void>) () -> {
            Segment externalEvent = startSegment(new Outbound());
            finishExternalEvent(externalEvent, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            return null;
        });

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.example.com/library/operation");
        verifyUnscopedMetricsPresent("External/www.example.com/all",
                "External/allOther", "External/all");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT);
        verifyNoExceptions();
    }

    @Test
    public void testFailure() throws Exception {
        long time = doInTransaction((Callable<Void>) () -> {
            Segment externalEvent = startSegment(new Outbound());
            Throwable error = new Exception();
            finishExternalEvent(externalEvent, error, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            return null;
        });

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.example.com/library/operation");
        verifyUnscopedMetricsPresent("External/www.example.com/all",
                "External/allOther", "External/all");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT);
        verifyNoExceptions(); // ??
    }

    @Test
    public void testIgnoredTransaction() throws Exception {
        doInTransaction((Callable<Void>) () -> {
            NewRelic.getAgent().getTransaction().ignore();
            Segment externalEvent = NewRelic.getAgent().getTransaction().startSegment("foo");
            Assert.assertEquals(externalEvent, NoOpSegment.INSTANCE);
            return null;
        });
        verifyTimesSet(0);
    }

    @Test
    public void testNoTransaction() {
        Segment externalEvent = NewRelic.getAgent().getTransaction().startSegment("foo");
        Assert.assertEquals(externalEvent, NoOpSegment.INSTANCE);
        verifyTimesSet(0);
    }

    @Test
    public void testTwoIntermixed() throws Exception {
        long time = doInTransaction((Callable<Void>) () -> {
            Segment externalEvent1 = startSegment(new Outbound());
            Segment externalEvent2 = startSegment(new Outbound());
            finishExternalEvent(externalEvent1, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
            finishExternalEvent(externalEvent2, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            return null;
        });

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                2, "External/www.example.com/library/operation");
        verifyUnscopedMetricsPresent(stats, 2, "External/www.example.com/all",
                "External/allOther", "External/all");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT,
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT);
        verifyNoExceptions();
    }

    @Test
    public void testFinishTwice() throws Exception {
        doInTransaction((Callable<Void>) () -> {
            Segment externalEvent1 = startSegment(new Outbound());
            finishExternalEvent(externalEvent1, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            finishExternalEvent(externalEvent1, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            return null;
        });

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.example.com/library/operation");
        verifyUnscopedMetricsPresent("External/www.example.com/all",
                "External/allOther", "External/all");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.host2.com/library");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT);
        verifyNoExceptions();
    }

    // with the refactoring of the transaction object, we are no longer one to one meaning this is allowed
    @Test
    public void testStartAfterFinish() throws Exception {
        doInTransaction((Callable<Void>) () -> {
            Segment externalEvent1 = startSegment(new Outbound());
            finishExternalEvent(externalEvent1, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            Segment externalEvent2 = startSegment(new Outbound());
            finishExternalEvent(externalEvent2, null, new Inbound(), HOST,
                    LIBRARY, URI, OPERATION_NAME);
            return null;
        });

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                2, "External/www.example.com/library/operation");
        verifyUnscopedMetricsPresent(2, "External/www.example.com/all",
                "External/allOther", "External/all");
        verifyScopedMetricsNotPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.host2.com/library");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT,
                "External/www.example.com/library/operation", NO_ASYNC_CONTEXT);
        verifyNoExceptions();
    }

    @Test
    public void testCat() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            final Outbound outbound = new Outbound();
            final Inbound inbound = new Inbound();
            doInTransaction((Callable<Void>) () -> {
                Segment externalEvent = startSegment(outbound);
                Transaction transaction = Transaction.getTransaction(false);
                String encodingKey = transaction.getCrossProcessConfig().getEncodingKey();
                String appData = Obfuscator.obfuscateNameUsingKey(
                        "[\"crossProcessId\",\"externalTransactionName\"]",
                        encodingKey);
                inbound.headers.put(HeadersUtil.NEWRELIC_APP_DATA_HEADER, appData);
                finishExternalEvent(externalEvent, null, inbound, HOST, LIBRARY, URI, OPERATION_NAME);
                return null;
            });

            // assert outbound request headers were populated correctly
            Assert.assertTrue(outbound.headers
                    .containsKey(HeadersUtil.NEWRELIC_ID_HEADER));
            Assert.assertTrue(outbound.headers
                    .containsKey(HeadersUtil.NEWRELIC_TRANSACTION_HEADER));

            // assert inbound response headers were processed correctly by checking for CAT metric name
            verifyTimesSet(1);
            verifyScopedMetricsPresent(
                    "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                    "ExternalTransaction/www.example.com/crossProcessId/externalTransactionName");
            verifyUnscopedMetricsPresent("External/www.example.com/all",
                    "External/allOther", "External/all");
            verifyScopedMetricsNotPresent(
                    "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                    "External/www.host2.com/library");
            verifyTransactionSegmentsBreadthFirst(
                    "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                    "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                    Thread.currentThread().getName(),
                    "ExternalTransaction/www.example.com/crossProcessId/externalTransactionName",
                    NO_ASYNC_CONTEXT);
            verifyNoExceptions();
        } finally {
            holder.close();
        }
    }

    @Test
    public void testWithRegisterStart() throws Exception {
        final String[] asyncClassAndThreadName = new String[2];
        long time = doInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // register a new async activity
                Token token = NewRelic.getAgent().getTransaction().getToken();
                Assert.assertTrue(token.isActive());

                // submit async activity
                executorService.submit(new Callable<Void>() {
                    @Trace(async = true)
                    public Void call() throws Exception {
                        asyncClassAndThreadName[0] = getClass().getName();
                        asyncClassAndThreadName[1] = Thread.currentThread()
                                .getName();

                        // start async activity
                        Assert.assertTrue(token.linkAndExpire());

                        // start external work
                        final Segment externalEvent = startSegment(new Outbound());

                        // submit external work
                        executorService.submit((Callable<Void>) () -> {
                            // finish external work
                            finishExternalEvent(externalEvent, null,
                                    new Inbound(), HOST, LIBRARY, URI,
                                    OPERATION_NAME);
                            return null;
                        }).get();
                        return null;
                    }
                }).get();
                return null;
            }
        });

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/" + asyncClassAndThreadName[0] + "/call",
                "External/www.example.com/library/operation");
        verifyUnscopedMetricsPresent("External/www.example.com/all",
                "External/allOther", "External/all");
        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(), "Java/"
                        + asyncClassAndThreadName[0] + "/call",
                asyncClassAndThreadName[1],
                "External/www.example.com/library/operation", "segment-api");
        verifyNoExceptions();
    }

    @Test
    public void testConcurrentSuccess() throws Exception {
        doInTransaction((Callable<Void>) () -> {
            // pretend some external calls are started in a tx
            final Segment externalEvent1 = startSegment(new Outbound());
            final Segment externalEvent2 = startSegment(new Outbound());

            // pretend they're finished asynchronously
            executorService.submit((Callable<Void>) () -> {
                Thread.sleep(500);
                finishExternalEvent(externalEvent1, null,
                        new Inbound(), "http://www.one.com", LIBRARY,
                        "http://www.one.com/path", "one");
                return null;
            });

            executorService.submit((Callable<Void>) () -> {
                // finish external work
                finishExternalEvent(externalEvent2, null,
                        new Inbound(), "http://www.two.com", LIBRARY,
                        "http://www.two.com/path", "two");
                return null;
            });

            return null;
        });
        executorService.shutdown();
        executorService.awaitTermination(1500, TimeUnit.MILLISECONDS);

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.one.com/library/one",
                "External/www.two.com/library/two");

        verifyUnscopedMetricsPresentIgnoringValues(stats,
                "External/www.one.com/all", "External/www.two.com/all",
                "External/allOther", "External/all");
        verifyStatsSingleCount(stats.getUnscopedStats(),
                "External/www.one.com/all", "External/www.two.com/all");
        verifyStatsCount(stats.getUnscopedStats(), 2, "External/allOther",
                "External/all");

        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.one.com/library/one", "segment-api",
                "External/www.two.com/library/two", "segment-api");
        verifyNoExceptions();
    }

    @Test
    public void testConcurrentFailure() throws Exception {
        doInTransaction((Callable<Void>) () -> {
            // pretend some external calls are started in a tx
            final Segment externalEvent1 = startSegment(new Outbound());
            final Segment externalEvent2 = startSegment(new Outbound());
            // pretend they're finished asynchronously
            executorService.submit((Callable<Void>) () -> {
                Thread.sleep(500);
                finishExternalEvent(externalEvent1, null,
                        new Inbound(), "http://www.one.com", LIBRARY,
                        "http://www.one.com/path", "one");
                return null;
            });

            executorService.submit((Callable<Void>) () -> {
                // finish external work
                Throwable throwable = new RuntimeException(
                        "SomeException");
                finishExternalEvent(externalEvent2, throwable,
                        new Inbound(), "http://www.two.com", LIBRARY,
                        "http://www.two.com/path", "two");
                return null;
            });

            return null;
        });
        executorService.shutdown();
        executorService.awaitTermination(1500, TimeUnit.MILLISECONDS);

        verifyTimesSet(1);
        verifyScopedMetricsPresent(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "External/www.one.com/library/one",
                "External/www.two.com/library/two");

        verifyUnscopedMetricsPresentIgnoringValues(stats,
                "External/www.one.com/all", "External/www.two.com/all",
                "External/allOther", "External/all");
        verifyStatsSingleCount(stats.getUnscopedStats(),
                "External/www.one.com/all", "External/www.two.com/all");
        verifyStatsCount(stats.getUnscopedStats(), 2, "External/allOther",
                "External/all");

        verifyTransactionSegmentsBreadthFirst(
                "OtherTransaction/Custom/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                "Java/com.newrelic.agent.async.ExternalAsyncTest/doInTransaction",
                Thread.currentThread().getName(),
                "External/www.one.com/library/one", "segment-api",
                "External/www.two.com/library/two", "segment-api");
        verifyNoExceptions();
    }

    @Trace(dispatcher = true)
    private long doInTransaction(Callable<?> work) throws Exception {
        long startTime = ServiceFactory.getTransactionTraceService()
                .getThreadMXBean().getCurrentThreadCpuTime();
        work.call();
        long endTime = ServiceFactory.getTransactionTraceService()
                .getThreadMXBean().getCurrentThreadCpuTime();
        return endTime - startTime;
    }

    @Override
    public void verifyCpu(long minCpu) {
        super.verifyCpu(minCpu);

        Collection<Tracer> tracers = data.getTracers();
        for (Tracer current : tracers) {
            if (current instanceof TransactionActivityInitiator) {
                TransactionActivity txa = current.getTransactionActivity();
                if (txa.getRootTracer().getAgentAttribute("async_context")
                        .equals(ASYNC_TXA_NAME)) {
                    Assert.assertEquals(
                            "External calls should have zero CPU time", 0L,
                            txa.getTotalCpuTime());
                }
            }
        }
    }

    private static class Inbound extends ExtendedInboundHeaders {

        public final Map<String, String> headers = new HashMap<>();

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }
    }

    private static class Outbound implements OutboundHeaders {

        public final Map<String, String> headers = new HashMap<>();

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, value);
        }
    }
}
