/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.*;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.*;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.errors.ErrorAnalyzerImpl;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.OtherTransSimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.api.agent.NewRelic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.newrelic.agent.config.SpanEventsConfig.SERVER_SPAN_HARVEST_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class SpanEventsServiceTest {

    private final String APP_NAME = "Unit Test";

    MockServiceManager serviceManager;
    @Mock
    public SpanEventCreationDecider spanEventCreationDecider;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        serviceManager = new MockServiceManager();

        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(AgentConfigImpl.APP_NAME, APP_NAME);
        localSettings.put("distributed_tracing", Collections.singletonMap("enabled", true));
        localSettings.put("span_events", Collections.singletonMap("collect_span_events", true));
        when(spanEventCreationDecider.shouldCreateSpans(any(TransactionData.class))).thenReturn(true);

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, localSettings, new HashMap<String, Object>());
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, localSettings);
        serviceManager.setConfigService(configService);
        ServiceFactory.setServiceManager(serviceManager);

        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setThreadService(new ThreadService());
        final MockSpanEventReservoirManager reservoirManager = new MockSpanEventReservoirManager(configService);
        Consumer<SpanEvent> backendConsumer = spanEvent -> reservoirManager.getOrCreateReservoir(APP_NAME).add(spanEvent);

        SpanErrorBuilder defaultSpanErrorBuilder = new SpanErrorBuilder(
                new ErrorAnalyzerImpl(agentConfig.getErrorCollectorConfig()),
                new ErrorMessageReplacer(agentConfig.getStripExceptionConfig()));

        Map<String, SpanErrorBuilder> map = new HashMap<>();
        map.put(agentConfig.getApplicationName(), defaultSpanErrorBuilder);

        EnvironmentService environmentService = mock(EnvironmentService.class, RETURNS_DEEP_STUBS);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        when(transactionDataToDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), anyBoolean()))
                .thenReturn(Collections.<String, Object>emptyMap());
        TracerToSpanEvent tracerToSpanEvent = new TracerToSpanEvent(map, environmentService, transactionDataToDistributedTraceIntrinsics, defaultSpanErrorBuilder);
        SpanEventsServiceImpl spanEventsService = SpanEventsServiceImpl.builder()
                .agentConfig(agentConfig)
                .reservoirManager(reservoirManager)
                .collectorSender(mock(CollectorSpanEventSender.class))
                .eventBackendStorage(backendConsumer)
                .spanEventCreationDecider(spanEventCreationDecider)
                .tracerToSpanEvent(tracerToSpanEvent)
                .build();
        serviceManager.setSpansEventService(spanEventsService);
        serviceManager.setAttributesService(new AttributesService());
    }

    @After
    public void after() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void testSpanEvent() {
        TransactionData transactionData = new TransactionDataTestBuilder(
                APP_NAME,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                new MockDispatcherTracer())
                .setTracers(Collections.<Tracer>emptyList())
                .build();

        Transaction mockTransaction = transactionData.getTransaction();
        when(mockTransaction.sampled()).thenReturn(true);
        when(mockTransaction.getPriority()).thenReturn(1.5f);

        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) ServiceFactory.getSpanEventService();
        spanEventsService.dispatcherTransactionFinished(transactionData, new TransactionStats());

        SamplingPriorityQueue<SpanEvent> reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertEquals(1, reservoir.getTotalSampledPriorityEvents());
    }

    @Test
    public void testMaxSamplesStored() {
        SpanEventsService spanEventsService = serviceManager.getSpanEventsService();

        spanEventsService.setMaxSamplesStored(0);

        final SpanEvent event = new SpanEventFactory(APP_NAME)
                .setCategory(SpanCategory.generic)
                .setPriority(1.23f)
                .setDurationInSeconds(1.3f)
                .setServerAddress("yourHost")
                .setTraceId("gnisnacirema")
                .setGuid("globallyuniqueidentifier")
                .setSampled(true)
                .build();
        spanEventsService.storeEvent(event);

        SamplingPriorityQueue<SpanEvent> reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertEquals(0, reservoir.size());

        spanEventsService.setMaxSamplesStored(2);

        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);

        reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertEquals(2, reservoir.size());

        spanEventsService.setMaxSamplesStored(13);

        for (int i = 0; i < 100; i++) {
            spanEventsService.storeEvent(event);
        }

        reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertEquals(13, reservoir.size());
    }

    @Test
    public void testDoesNotCreateSpansIfToldNotTo() {
        TransactionData transactionData = new TransactionDataTestBuilder(
                APP_NAME,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                new MockDispatcherTracer())
                .setTracers(Collections.<Tracer>emptyList())
                .build();

        Transaction mockTransaction = transactionData.getTransaction();
        when(mockTransaction.sampled()).thenThrow(new AssertionError("should not have been called"));
        when(mockTransaction.getPriority()).thenThrow(new AssertionError("should not have been called"));

        when(spanEventCreationDecider.shouldCreateSpans(transactionData)).thenReturn(false);

        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) ServiceFactory.getSpanEventService();
        spanEventsService.dispatcherTransactionFinished(transactionData, null);

        SamplingPriorityQueue<SpanEvent> reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
        assertEquals(0, reservoir.getTotalSampledPriorityEvents());
    }

    // should become: (with all attributes intact)
    // entry span to service A (root)
    //   exit span 1 to service B
    //   exit span 2 to service B
    //   exit span 1 to service C
    //   LLM Span
    //       exit span to service D
    @Test
    public void testPartialGranularity_Reduced() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            SamplingPriorityQueue<SpanEvent> reservoir = runPartialGranularityTest(Transaction.PartialSampleType.REDUCED);

            assertEquals(6, reservoir.getTotalSampledPriorityEvents());
            assertAllPartialGranularitySpans(reservoir, true, false);

            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/DistributedTrace/PartialGranularity/REDUCED/Span/Instrumented"), eq(7.0f)));
            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/DistributedTrace/PartialGranularity/REDUCED/Span/Kept"), eq(6.0f)));
        }
    }

    // should become: (with non-essential attributes removed)
    // entry span to service A (root)
    //   exit span 1 to service B
    //   exit span 2 to service B
    //   exit span 1 to service C
    //   LLM Span
    //       exit span to service D
    @Test
    public void testPartialGranularity_Essential() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            SamplingPriorityQueue<SpanEvent> reservoir = runPartialGranularityTest(Transaction.PartialSampleType.ESSENTIAL);

            assertEquals(6, reservoir.getTotalSampledPriorityEvents());
            assertAllPartialGranularitySpans(reservoir, false, false);

            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/DistributedTrace/PartialGranularity/ESSENTIAL/Span/Instrumented"), eq(7.0f)));
            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/DistributedTrace/PartialGranularity/ESSENTIAL/Span/Kept"), eq(6.0f)));
        }
    }

    // should become:
    // entry span to service A (root)
    //   exit span 1 to service B (with nr.ids and nr.durations attrs that incorporate exit span 2 to service B)
    //   exit span 1 to service C
    //   LLM Span
    //   exit span to service D  (note: everything is re-parented to the root span for COMPACT)
    @Test
    public void testPartialGranularity_Compact() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            SamplingPriorityQueue<SpanEvent> reservoir = runPartialGranularityTest(Transaction.PartialSampleType.COMPACT);

            outputSpans(reservoir);

            assertEquals(5, reservoir.getTotalSampledPriorityEvents());
            assertAllPartialGranularitySpans(reservoir, false, true);

            boolean hadSpanWithNRIDsAttr = false;
            boolean hadSpanWithNRDurationAttr = false;
            for (SpanEvent span : reservoir.asList()) {
                if (span.getAgentAttributes() == null) continue;
                for (String attr : span.getAgentAttributes().keySet()) {
                    if ("nr.ids".equals(attr)) hadSpanWithNRIDsAttr = true;
                    if ("nr.ids".equals(attr)) hadSpanWithNRDurationAttr = true;
                }
            }
            assertEquals(true, hadSpanWithNRIDsAttr);
            assertEquals(true, hadSpanWithNRDurationAttr);

            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/DistributedTrace/PartialGranularity/COMPACT/Span/Instrumented"), eq(7.0f)));
            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/DistributedTrace/PartialGranularity/COMPACT/Span/Kept"), eq(5.0f)));
        }
    }

    private void outputSpans (SamplingPriorityQueue<SpanEvent> reservoir) {
        if (reservoir == null || reservoir.size() == 0) {
            System.out.println("No spans to output");
            return;
        }
        System.out.println("Outputting "+reservoir.size()+" spans");
        for (SpanEvent span : reservoir.asList()) {
            System.out.print("Span: "+span.getName()+"[");
            if (span.getAgentAttributes() != null) {
                boolean first = true;
                for (String attr : span.getAgentAttributes().keySet()) {
                    if (!first) System.out.print(", ");
                    System.out.print("'"+attr+"': '"+span.getAgentAttributes().get(attr)+"'");
                    first = false;
                }
            }
            System.out.println("]");
        }
    }

    @Test
    public void spanEventsServiceMaxSamplesStoredRespectsServerSide() {
        //given
        MockRPMService mockRPMService = new MockRPMService();
        mockRPMService.setApplicationName(APP_NAME);
        RPMServiceManager mockRPMServiceManager = mock(RPMServiceManager.class);
        when(mockRPMServiceManager.getRPMService()).thenReturn(mockRPMService);
        serviceManager.setRPMServiceManager(mockRPMServiceManager);
        serviceManager.setHarvestService(new HarvestServiceImpl());
        SpanEventsService spanEventsService = serviceManager.getSpanEventsService();
        spanEventsService.addHarvestableToService(APP_NAME);
        HarvestServiceImpl harvestService = (HarvestServiceImpl) serviceManager.getHarvestService();

        Map<String, Object> connectionInfo = new HashMap<>();
        Map<String, Object> eventHarvest = new HashMap<>();
        Map<String, Object> harvestLimits = new HashMap<>();
        eventHarvest.put("report_period_ms", 60000L);
        long maxSamples = 3L;
        eventHarvest.put(SpanEventsConfig.SERVER_SPAN_HARVEST_LIMIT, maxSamples);
        connectionInfo.put(SERVER_SPAN_HARVEST_CONFIG, eventHarvest);
        //when
        harvestService.startHarvestables(ServiceFactory.getRPMService(), AgentConfigImpl.createAgentConfig(connectionInfo));
        //then
        assertEquals("max samples stored should be: " + maxSamples, maxSamples, spanEventsService.getMaxSamplesStored());
    }

    private void assertAllPartialGranularitySpans(SamplingPriorityQueue<SpanEvent> reservoir, boolean shouldNonEssentialAttrsBeThere, boolean compactMode) {
        SpanEvent rootSpan = reservoir.asList().stream()
                .filter(span ->
                        "Java/com.newrelic.agent.service.analytics.SpanEventsServiceTest/root".equals(span.getIntrinsics().get("name")))
                .findFirst()
                .orElse(null);
        assertNotNull(rootSpan);
        SpanEvent llmSpan = null;
        SpanEvent externalDSpan = null;
        for (SpanEvent span : reservoir.asList()) {
            if ("Llm/SOMETHING/function".equals(span.getIntrinsics().get("name"))) {
                llmSpan = span;
            } else if ("service D".equals(span.getAgentAttributes().get("http.url"))) {
                externalDSpan = span;
            }
            if (span == rootSpan) {
                // we should still have the essential attribute(s)
                assertNotNull("Essential attributes should be kept on the root span",
                        span.getAgentAttributes().get("error.class"));
                // make sure non-essential attributes were either stripped or kept, as the case dictates
                assertEquals("Non-essential attributes should "+(shouldNonEssentialAttrsBeThere ? "" : "NOT ")+"be kept on the root span",
                        shouldNonEssentialAttrsBeThere, span.getAgentAttributes().get("non-essential") != null);
            } else {
                // make sure we still have the essential attribute we added to each non-root span
                // http.url is only used here as an example, it's not actually always on external spans
                // there will be no http.url attr on the llm span
                if (span != llmSpan) {
                    assertNotNull("Essential attributes should be kept on non-root spans",
                            span.getAgentAttributes().get("http.url"));
                }
                // make sure each non-root span has a parent ID pointing to the root span
                // external D should have a parent of the LLM span (checked below) unless we are in COMPACT mode
                if (span != externalDSpan || compactMode) {
                    assertEquals("Non-root spans should all have a parent ID equal to the root span guid",
                            span.getParentId(), rootSpan.getGuid());
                }
                // make sure non-essential attributes were either stripped or kept, as the case dictates
                assertEquals("Non-essential attributes should "+(shouldNonEssentialAttrsBeThere ? "" : "NOT")+" be kept on the non-root spans",
                        shouldNonEssentialAttrsBeThere, span.getAgentAttributes().get("non-essential") != null);
            }
        }
        // if this is external span D, it's parent should NOT be the root span, but rather the llm span
        // UNLESS we are in compact mode
        assertNotNull(llmSpan);
        assertNotNull(externalDSpan);
        if (!compactMode) {
            assertEquals(llmSpan.getGuid(), externalDSpan.getParentId());
        }
    }

    private SamplingPriorityQueue<SpanEvent> runPartialGranularityTest(Transaction.PartialSampleType partialSampleType) {
        Transaction tx = mock(Transaction.class);

        TransactionActivity txa = mock(TransactionActivity.class);
        when(txa.getTransaction()).thenReturn(tx);
        when(txa.canCreateTransactionSegment()).thenReturn(true);
        when(tx.getTransactionActivity()).thenReturn(txa);

        TransactionState txState = mock(TransactionState.class);
        when(txState.finish(Mockito.any(), Mockito.any())).thenReturn(true);
        when(tx.getTransactionState()).thenReturn(txState);

        TransactionCounts txCounts = mock(TransactionCounts.class);
        when(txCounts.isOverTracerSegmentLimit()).thenReturn(false);
        when(tx.getTransactionCounts()).thenReturn(txCounts);

        List<Tracer> tracers = buildTracersForPartialGranularity(tx);

        Tracer rootTracer = tracers.get(0);
        tracers = tracers.subList(1, tracers.size());
        TransactionData transactionData = new TransactionDataTestBuilder(
                APP_NAME,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                rootTracer)
                .setTx(tx)
                .setTracers(tracers)
                .setPartialSampleType(partialSampleType)
                .build();

        Transaction mockTransaction = transactionData.getTransaction();
        when(mockTransaction.sampled()).thenReturn(true);
        when(mockTransaction.getPriority()).thenReturn(1.5f);

        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) ServiceFactory.getSpanEventService();
        spanEventsService.dispatcherTransactionFinished(transactionData, new TransactionStats());

        return spanEventsService.getOrCreateDistributedSamplingReservoir(APP_NAME);
    }

    // this code should build a series of tracers that would result in a full granularity span that looks like this:
    // entry span to service A (root)
    //   exit span 1 to service B
    //   inProcess function trace span
    //       exit span 2 to service B
    //   exit span 1 to service C
    //   LLM Span
    //       exit span to service D
    private List<Tracer> buildTracersForPartialGranularity(Transaction tx) {
        Tracer rootTracer = new OtherRootTracer(tx, new ClassMethodSignature("Test", "root", "()V"), this,
                new OtherTransSimpleMetricNameFormat("myMetricName"));
        DefaultTracer externalB1Tracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "service B", "()V"), this);
        DefaultTracer inProcessTracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "inProcess", "()V"), this);
        DefaultTracer externalB2Tracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "service B", "()V"), this);
        DefaultTracer externalCTracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "service C", "()V"), this);
        DefaultTracer llmTracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "llm", "()V"), this);
        DefaultTracer externalDTracer = new DefaultTracer(tx, new ClassMethodSignature("Test", "service D", "()V"), this);

        // add attributes to the root span, 1 essential, 1 not
        rootTracer.setAgentAttribute("error.class", "MyClass", true);
        rootTracer.setAgentAttribute("non-essential", "boo", true);

        // add the necessary agent attributes to keep the 3 external spans
        externalB1Tracer.setAgentAttribute("http.url", "service B", true);
        externalB2Tracer.setAgentAttribute("http.url", "service B", true);
        externalCTracer.setAgentAttribute("http.url", "service C", true);
        llmTracer.setMetricName("Llm/SOMETHING/function");
        externalDTracer.setAgentAttribute("http.url", "service D", true);

        // add a non-essential agent attribute to be removed when required
        externalB1Tracer.setAgentAttribute("non-essential", "how dare you!", true);
        externalB2Tracer.setAgentAttribute("non-essential", "YOU'RE NOT ESSENTIAL!", true);
        externalCTracer.setAgentAttribute("non-essential", "you heard me!", true);
        llmTracer.setAgentAttribute("non-essential", "non-essential ai? never!", true);
        externalDTracer.setAgentAttribute("non-essential", "why can't i stay?", true);

        // start the root tracer
        tx.getTransactionActivity().tracerStarted(rootTracer);

        // start an external call to service B and finish it
        tx.getTransactionActivity().tracerStarted(externalB1Tracer);
        externalB1Tracer.setParentTracer(rootTracer);
        externalB1Tracer.finish(0, null);

        // start an in-process tracer that will call external service B again and then finish both
        tx.getTransactionActivity().tracerStarted(inProcessTracer);
        tx.getTransactionActivity().tracerStarted(externalB2Tracer);
        externalB2Tracer.finish(0, null);
        externalB2Tracer.setParentTracer(inProcessTracer);
        inProcessTracer.finish(0, null);
        inProcessTracer.setParentTracer(rootTracer);

        // start an external call to service C and finish it
        tx.getTransactionActivity().tracerStarted(externalCTracer);
        externalCTracer.finish(0, null);
        externalCTracer.setParentTracer(rootTracer);

        // start the LLM span that will call service D and finish both
        tx.getTransactionActivity().tracerStarted(llmTracer);
        tx.getTransactionActivity().tracerStarted(externalDTracer);
        externalDTracer.finish(0, null);
        externalDTracer.setParentTracer(llmTracer);
        llmTracer.finish(0, null);
        llmTracer.setParentTracer(rootTracer);

        rootTracer.finish(0, null);

        return Arrays.asList(rootTracer, externalB1Tracer, inProcessTracer, externalB2Tracer, externalCTracer, llmTracer, externalDTracer);
    }
}
