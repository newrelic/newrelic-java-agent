package com.newrelic;

import com.newrelic.agent.model.SpanEvent;

import java.util.Collection;
import java.util.Map;

public interface Exporter {

    void updateMetadata(String agentRunToken, Map<String, String> requestMetadata);

    boolean isReady();

    int maxExportSize();

    void export(Collection<SpanEvent> spanEvents);

    void shutdown();

}
