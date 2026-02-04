package com.agent.instrumentation.awsjavasdk2.services.s3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.services.s3.model.S3Response;

import java.util.function.BiConsumer;

public class S3ResponseResultWrapper<T extends S3Response, U> implements BiConsumer<T, U> {
    private Segment segment;
    private String uri;
    private String operationName;

    public S3ResponseResultWrapper(Segment segment, String uri, String operationName) {
        this.segment = segment;
        this.uri = uri;
        this.operationName = operationName;
    }

    @Override
    public void accept(T s3Response, U u) {
        try {
            S3MetricUtil.reportExternalMetrics(segment, uri, s3Response, operationName);
            segment.end();
        } catch (Throwable t1) {
            AgentBridge.instrumentation.noticeInstrumentationError(t1, Weaver.getImplementationTitle());
        }
    }
}
