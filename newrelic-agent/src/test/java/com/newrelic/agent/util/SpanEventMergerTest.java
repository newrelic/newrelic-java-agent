package com.newrelic.agent.util;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.InsightsConfigImpl;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

public class SpanEventMergerTest {

    private static double MAX_DURATION_DELTA = 0.0000001; // precision problems when casting float to double

    @BeforeClass
    public static void setup() {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> customEvents = new HashMap<>();
        root.put("custom_insights_events", customEvents);
        customEvents.put(InsightsConfigImpl.MAX_ATTRIBUTE_VALUE, 255);
        MockServiceManager mockServiceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(Mockito.mock(ConfigService.class));

        // start with the default
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertTrue(config.getInsightsConfig().isEnabled());
    }

    @Test
    public void spanEvent_merge_multipleGroups_multipleScenarios() {
        List<SpanEvent> allOverlappingGroup = generateMergableSpanEvents("group1", 3,
                new Long[]{ 100L, 200L, 300L },
                new Float[]{ 0.2f, 0.2f, 0.2f });
        List<SpanEvent> nonOverlappingGroup = generateMergableSpanEvents("group2", 3,
                new Long[]{ 1100L, 2100L, 3100L },
                new Float[]{ 0.2f, 0.3f, 0.4f });
        List<SpanEvent> mixedOverlapGroup = generateMergableSpanEvents("group3", 3,
                new Long[]{ 4100L, 4200L, 5200L },
                new Float[]{ 0.2f, 0.3f, 0.7f });
        List<SpanEvent> outOfOrderGroup = generateMergableSpanEvents("group4", 3,
                new Long[]{ 7000L, 8000L, 6000L },
                new Float[]{ 0.2f, 0.9f, 0.5f });
        List<SpanEvent> singleSpanGroup = generateMergableSpanEvents("group5", 1,
                new Long[]{ 9000L },
                new Float[]{ 0.6f });

        List<SpanEvent> allSpans = new ArrayList<>();
        allSpans.addAll(allOverlappingGroup);
        allSpans.addAll(nonOverlappingGroup);
        allSpans.addAll(mixedOverlapGroup);
        allSpans.addAll(outOfOrderGroup);
        allSpans.addAll(singleSpanGroup);

        List<SpanEvent> mergedSpans = SpanEventMerger.findGroupsAndMergeSpans(allSpans, true);

        assertEquals(5, mergedSpans.size());

        SpanEvent group1Span = mergedSpans.get(0);
        assertEquals(0.4, (Double)group1Span.getAgentAttributes().get("nr.durations"), MAX_DURATION_DELTA);
        assertEquals("group1-0", group1Span.getGuid());
        assertEquals(true, ((List<String>)group1Span.getAgentAttributes().get("nr.ids")).contains("group1-1"));
        assertEquals(true, ((List<String>)group1Span.getAgentAttributes().get("nr.ids")).contains("group1-2"));

        SpanEvent group2Span = mergedSpans.get(1);
        assertEquals(0.9, (Double)group2Span.getAgentAttributes().get("nr.durations"), MAX_DURATION_DELTA);
        assertEquals("group2-0", group2Span.getGuid());
        assertEquals(true, ((List<String>)group2Span.getAgentAttributes().get("nr.ids")).contains("group2-1"));
        assertEquals(true, ((List<String>)group2Span.getAgentAttributes().get("nr.ids")).contains("group2-2"));

        SpanEvent group3Span = mergedSpans.get(2);
        assertEquals(1.1, (Double)group3Span.getAgentAttributes().get("nr.durations"), MAX_DURATION_DELTA);
        assertEquals("group3-0", group3Span.getGuid());
        assertEquals(true, ((List<String>)group3Span.getAgentAttributes().get("nr.ids")).contains("group3-1"));
        assertEquals(true, ((List<String>)group3Span.getAgentAttributes().get("nr.ids")).contains("group3-2"));

        SpanEvent group4Span = mergedSpans.get(3);
        assertEquals(1.6, (Double)group4Span.getAgentAttributes().get("nr.durations"), MAX_DURATION_DELTA);
        assertEquals("group4-2", group4Span.getGuid());
        assertEquals(true, ((List<String>)group4Span.getAgentAttributes().get("nr.ids")).contains("group4-0"));
        assertEquals(true, ((List<String>)group4Span.getAgentAttributes().get("nr.ids")).contains("group4-1"));

        SpanEvent group5Span = mergedSpans.get(4);
        assertEquals(false, group5Span.getAgentAttributes().containsKey("nr.durations"));
        assertEquals(false, group5Span.getAgentAttributes().containsKey("nr.ids"));
        assertEquals("group5-0", group5Span.getGuid());
    }

    @Test
    public void spanEvent_merge_moreGuidsThanCanFitInNrIds() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {
            int numSpansToAdd = SpanEventMerger.MAX_NR_IDS + 10;
            Long[] timestamps = new Long[numSpansToAdd];
            Float[] durations = new Float[numSpansToAdd];
            for (int i = 0;i<numSpansToAdd;i++) {
                timestamps[i] = (long) (i*100);
                durations[i] = 0.05f;
            }
            List<SpanEvent> spans = generateMergableSpanEvents("myprefix", numSpansToAdd, timestamps, durations);

            List<SpanEvent> mergedSpans = SpanEventMerger.findGroupsAndMergeSpans(spans, true);

            assertEquals(1, mergedSpans.size());
            assertEquals(SpanEventMerger.MAX_NR_IDS, ((List<String>)mergedSpans.get(0).getAgentAttributes().get("nr.ids")).size());

            // no idea why this is saying it's a float instead of an int, but whatever
            newRelic.verify(() -> NewRelic.recordMetric(eq("Supportability/Java/PartialGranularity/NrIds/Dropped"), eq(9.0f)));
        }
    }

    private List<SpanEvent> generateMergableSpanEvents(String prefix, int count, Long[] timestamps, Float[] durations) {
        List<SpanEvent> spans = new ArrayList<>();
        for (int i=0;i<count;i++) {
            Map<String, Object> attrs = new HashMap<>();
            for (String attrName : SpanEvent.ENTITY_SYNTHESIS_ATTRS) {
                attrs.put(attrName, prefix + "-" + attrName + "value");
            }
            SpanEvent span = SpanEvent.builder()
                    .putIntrinsic("timestamp", timestamps[i])
                    .putIntrinsic("duration", durations[i])
                    .putIntrinsic("guid", prefix+"-"+i)
                    .putAllAgentAttributes(attrs).build();
            spans.add(span);
        }

        return spans;
    }

    @Test
    public void spanEvent_merge_matchingSpan_overlappingTimeSpan() {
        // span 1 starts at 100 and lasts 200 millis
        // span 2 starts at 200 and lasts 200 millis
        // total should be 300 millis
        runSumDurationsTest(100, 0.2f, 200, 0.2f, 0.3f);
    }

    @Test
    public void spanEvent_merge_matchingSpan_nonOverlappingTimeSpan() {
        // span 1 starts at 100 and lasts 200 millis
        // span 2 starts at 500 and lasts 200 millis
        // total should be 400 millis
        runSumDurationsTest(100, 0.2f, 500, 0.2f, 0.4f);
    }

    @Test
    public void spanEvent_mergeErrorAttrs_ignoreErrorPriority_withLaterSpan() {
        List<SpanEvent> results = runMergeErrorDataTest(100, 200, true);
        SpanEvent result = results.get(0);
        // merge should happen
        assertEquals("class2", result.getAgentAttributes().get("error.class"));
        assertEquals("message2", result.getAgentAttributes().get("error.message"));
        assertEquals(false, result.getAgentAttributes().get("error.expected"));
    }

    @Test
    public void spanEvent_mergeErrorAttrs_dontIgnoreErrorPriority_withLaterSpan() {
        List<SpanEvent> results = runMergeErrorDataTest(100, 200,false);
        SpanEvent result = results.get(0);
        // merge should not happen
        assertEquals("class1", result.getAgentAttributes().get("error.class"));
        assertEquals("message1", result.getAgentAttributes().get("error.message"));
        assertEquals(true, result.getAgentAttributes().get("error.expected"));
    }

    @Test
    public void spanEvent_mergeErrorAttrs_ignoreErrorPriority_withEarlierSpan() {
        List<SpanEvent> results = runMergeErrorDataTest(200, 100, true);
        SpanEvent result = results.get(0);
        // merge should not happen
        assertEquals("class1", result.getAgentAttributes().get("error.class"));
        assertEquals("message1", result.getAgentAttributes().get("error.message"));
        assertEquals(true, result.getAgentAttributes().get("error.expected"));
    }

    @Test
    public void spanEvent_mergeErrorAttrs_dontIgnoreErrorPriority_withEarlierSpan() {
        List<SpanEvent> results = runMergeErrorDataTest(200, 100, false);
        SpanEvent result = results.get(0);
        // merge should happen
        assertEquals("class2", result.getAgentAttributes().get("error.class"));
        assertEquals("message2", result.getAgentAttributes().get("error.message"));
        assertEquals(false, result.getAgentAttributes().get("error.expected"));
    }

    private List<SpanEvent> runMergeErrorDataTest(long timestamp1, long timestamp2, boolean ignoreErrorPriority) {
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put("http.url", "myurl");
        attrs1.put("error.class", "class1");
        attrs1.put("error.message", "message1");
        attrs1.put("error.expected", true);
        SpanEvent span1 = SpanEvent.builder()
                .putIntrinsic("timestamp", timestamp1)
                .putIntrinsic("duration", 100.0f)
                .putIntrinsic("guid", "myguid")
                .putAllAgentAttributes(attrs1).build();

        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put("http.url", "myurl");
        attrs2.put("error.class", "class2");
        attrs2.put("error.message", "message2");
        attrs2.put("error.expected", false);
        SpanEvent span2 = SpanEvent.builder()
                .putIntrinsic("timestamp", timestamp2)
                .putIntrinsic("guid", "myguid2")
                .putIntrinsic("duration", 100.0f)
                .putAllAgentAttributes(attrs2).build();

        List<SpanEvent> spans = new ArrayList<>();
        spans.add(span1);
        spans.add(span2);

        return SpanEventMerger.findGroupsAndMergeSpans(spans, ignoreErrorPriority);
    }

    private void runSumDurationsTest(long start1, float duration1, long start2, float duration2, double expectedNrDurations) {
        Map<String, String> entitySynthesisAttrs1 = new HashMap<>();
        entitySynthesisAttrs1.put("http.url", "myurl1");
        SpanEvent span1 = SpanEvent.builder()
                .putIntrinsic("timestamp", start1)
                .putIntrinsic("guid", "myguid")
                .putIntrinsic("duration", duration1)
                .putAllAgentAttributes(entitySynthesisAttrs1).build();


        Map<String, String> entitySynthesisAttrs2 = new HashMap<>();
        entitySynthesisAttrs2.put("http.url", "myurl1");
        SpanEvent span2 = SpanEvent.builder()
                .putIntrinsic("timestamp", start2)
                .putIntrinsic("guid", "myguid2")
                .putIntrinsic("duration", duration2)
                .putAllAgentAttributes(entitySynthesisAttrs2).build();

        assertEquals(true, span1.matchesEntitySynthesisAttrs(span2));

        List<SpanEvent> spans = new ArrayList<>();
        spans.add(span1);
        spans.add(span2);

        List<SpanEvent> mergedSpans = SpanEventMerger.findGroupsAndMergeSpans(spans, true);

        assertEquals(expectedNrDurations, (Double)mergedSpans.get(0).getAgentAttributes().get("nr.durations"), MAX_DURATION_DELTA);
    }

}
