package com.newrelic.agent.util;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.api.agent.NewRelic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpanEventMerger {

    // collector limit is 1024
    // minus 8 bytes for memory allocation for arrays
    // divided by 16 characters per GUID
    // (1024-8)/16 = 63.5
    // rounded down = 63
    public static final int MAX_NR_IDS = 63;

    public static List<SpanEvent> findGroupsAndMergeSpans(List<SpanEvent> spans, boolean ignoreErrorPriority) {
        List<List<SpanEvent>> groups = findGroups(spans);
        List<SpanEvent> mergedSpans = new ArrayList<>(groups.size());
        for (List<SpanEvent> group : groups) {
            if (group.size() == 1) { // nothing to actually merge, add it to the list and move to the next group
                mergedSpans.add(group.get(0));
                continue;
            }

            mergedSpans.add(mergeGroup(group, ignoreErrorPriority));
        }

        return mergedSpans;
    }

    private static SpanEvent mergeGroup (List<SpanEvent> group, boolean ignoreErrorPriority) {
        // get a list of start and end events for each span
        // we always want to merge everything into the first span (by timestamp)
        List<TimeFrameEvent> timeFrameEvents = new ArrayList<>(group.size()*2);
        SpanEvent mergedSpan = findFirstSpanInGroup(group);
        group.remove(mergedSpan);
        addTimeFrameEventsForSpan(timeFrameEvents, mergedSpan);
        // we do include this first span in the nr.durations value
        // we do NOT include the first span in the nr.ids value, since it's already in the guid for that span

        // keep track of which span to use the errors from
        SpanEvent errorSpanToUse = mergedSpan.hasAnyErrorAttrs() ? mergedSpan : null;
        List<String> nrIds = new ArrayList<>(group.size());

        int countIdsNotAddedToNRIds = 0;
        for (SpanEvent otherSpan : group) {
            if (nrIds.size() < MAX_NR_IDS) {
                nrIds.add(otherSpan.getGuid());
            } else {
                countIdsNotAddedToNRIds++;
            }
            addTimeFrameEventsForSpan(timeFrameEvents, otherSpan);
            errorSpanToUse = checkForErrorSpanToUse(errorSpanToUse, otherSpan, ignoreErrorPriority);
        }

        if (countIdsNotAddedToNRIds > 0) {
            NewRelic.recordMetric("Supportability/Java/PartialGranularity/NrIds/Dropped", countIdsNotAddedToNRIds);
        }

        // now add 2 new attributes to the first span that represent all the merged-in spans
        mergedSpan.getAgentAttributes().put("nr.ids", nrIds);
        float totalDuration = sumDurations(timeFrameEvents);
        mergedSpan.getAgentAttributes().put("nr.durations", totalDuration / 1000.0f); // we multiplied by 1000.0 when adding the TimeFrameEvents

        // if we found a span with errors, overwrite the merged spans error attrs with that span's values
        if (errorSpanToUse != null) {
            for (String attr : SpanEvent.ERROR_ATTRS) {
                if (!errorSpanToUse.getAgentAttributes().containsKey(attr)) {
                    mergedSpan.getAgentAttributes().remove(attr);
                } else {
                    Object value = errorSpanToUse.getAgentAttributes().get(attr);
                    mergedSpan.getAgentAttributes().put(attr, value);
                }
            }
        }

        return mergedSpan;
    }

    private static SpanEvent checkForErrorSpanToUse (SpanEvent errorSpanToUse, SpanEvent otherSpan, boolean ignoreErrorPriority) {
        if (otherSpan == null) return errorSpanToUse;

        if (otherSpan.hasAnyErrorAttrs()) {
            if (errorSpanToUse == null) errorSpanToUse = otherSpan;
            else {
                if (ignoreErrorPriority && otherSpan.getTimestamp() > errorSpanToUse.getTimestamp()) {
                    // if ignoreErrorPriority, then use the latest error
                    return otherSpan;
                } else if (!ignoreErrorPriority && otherSpan.getTimestamp() < errorSpanToUse.getTimestamp()) {
                    // if !ignoreErrorPriority, then use the earliest error
                    return otherSpan;
                }
            }
        }

        return errorSpanToUse;
    }

    // 1-dimensional sweep-line algorithm
    private static float sumDurations (List<TimeFrameEvent> timeFrameEvents) {
        // gotta be sorted, unfortunately
        timeFrameEvents.sort(Comparator.comparingDouble(TimeFrameEvent::getTimestamp));

        // calculate the total duration
        // used to track if we are inside of a timeframe that counts, >0 means we are
        // this effectively keeps track of how many spans' timeframes were are inside of
        int insideTimeFrameCount = 0;
        Float lastTimestamp = null;
        float totalDuration = 0.0f;
        for (TimeFrameEvent event : timeFrameEvents) {
            if (lastTimestamp != null && insideTimeFrameCount > 0) {
                totalDuration += (event.getTimestamp() - lastTimestamp);
            }
            insideTimeFrameCount += event.type.value; // add 1 for a start event, subtract 1 for an end event
            lastTimestamp = event.getTimestamp();
        }

        return totalDuration;
    }

    private static void addTimeFrameEventsForSpan (List<TimeFrameEvent> timeFrameEvents, SpanEvent span) {
        timeFrameEvents.add(new TimeFrameEvent(span.getTimestamp(), TimeFrameEventType.START));
        // duration is in seconds, timestamp is in milliseconds
        timeFrameEvents.add(new TimeFrameEvent((long) (span.getTimestamp() + (span.getDuration() * 1000.0f)),
                TimeFrameEventType.END));
    }

    private static SpanEvent findFirstSpanInGroup (List<SpanEvent> spans) {
        if (spans == null || spans.size() == 0) return null;

        SpanEvent result = null;
        for (SpanEvent span : spans) {
            if (result == null || span.getTimestamp() < result.getTimestamp()) result = span;
        }

        return result;
    }

    private static List<List<SpanEvent>> findGroups (List<SpanEvent> spans) {
        if (spans == null) return null;

        List<List<SpanEvent>> groups = new ArrayList<>();
        for (SpanEvent span : spans) {
            boolean foundGroup = false;
            for (List<SpanEvent> group : groups) {
                SpanEvent firstSpanInGroup = group.get(0); // if it matches the first, it also matches the rest
                if (span.matchesEntitySynthesisAttrs(firstSpanInGroup)) {
                    group.add(span);
                    foundGroup = true;
                }
            }
            if (!foundGroup) {
                List<SpanEvent> newGroup = new ArrayList<>();
                newGroup.add(span);
                groups.add(newGroup);
            }
        }

        return groups;
    }

    private enum TimeFrameEventType {
        START(1),
        END(-1);
        private final int value;
        TimeFrameEventType(int value) {
            this.value = value;
        }
    }

    private static class TimeFrameEvent {
        float timestamp;
        TimeFrameEventType type;
        TimeFrameEvent(float timestamp, TimeFrameEventType type) {
            this.timestamp = timestamp;
            this.type = type;
        }

        public float getTimestamp() {
            return timestamp;
        }
    }

}
