package com.agent.instrumentation.awsjavasdk2.services.firehose;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;

import java.util.concurrent.CompletableFuture;

public class SegmentHandler<T> {
    private final DeliveryStreamRawData streamRawData;
    private final CompletableFuture<T> completableFuture;
    private final Segment segment;
    private final String implementationTitle;

    public SegmentHandler(DeliveryStreamRawData streamRawData, CompletableFuture<T> completableFuture, Segment segment, String implementationTitle) {
        this.streamRawData = streamRawData;
        this.completableFuture = completableFuture;
        this.segment = segment;
        this.implementationTitle = implementationTitle;
    }

    public CompletableFuture<T> newSegmentCompletionStage() {
        if (completableFuture == null) {
            return null;
        }
        return completableFuture.whenComplete((r, t) -> {
            try {
                segment.reportAsExternal(FirehoseUtil.createCloudParams(streamRawData));
                segment.end();
            } catch (Throwable t1) {
                AgentBridge.instrumentation.noticeInstrumentationError(t1, implementationTitle);
            }
        });
    }
}