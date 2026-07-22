/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.converse.models.converse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.Segment;
import llm.converse.models.ModelInvocation;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;

import java.util.Map;
import java.util.function.Function;

/**
 * Wraps the supplied {@link ConverseStreamResponseHandler}, delegating every call
 * so the customer's own handling is unaffected, while tapping the event publisher to accumulate
 * the info needed to record LlmEvents once the stream completes or fails.
 */
public class ConverseStreamResponseHandlerWrapper implements ConverseStreamResponseHandler {
    private final ConverseStreamResponseHandler delegate;
    private final ConverseStreamRequest converseStreamRequest;
    private final Map<String, String> linkingMetadata;
    private final Map<String, Object> userAttributes;
    private final Segment segment;
    private final Token token;
    private final long startTime;
    private final String implementationTitle;
    private final ConverseStreamModelResponse modelResponse = new ConverseStreamModelResponse();

    private boolean firstEventSeen = false;
    private long timeToFirstToken = 0;

    public ConverseStreamResponseHandlerWrapper(ConverseStreamResponseHandler delegate, ConverseStreamRequest converseStreamRequest,
            Map<String, String> linkingMetadata, Map<String, Object> userAttributes, Segment segment, Token token, long startTime,
            String implementationTitle) {
        this.delegate = delegate;
        this.converseStreamRequest = converseStreamRequest;
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userAttributes;
        this.segment = segment;
        this.token = token;
        this.startTime = startTime;
        this.implementationTitle = implementationTitle;
    }

    @Override
    public void responseReceived(ConverseStreamResponse response) {
        modelResponse.applyHttpResponse(response);
        delegate.responseReceived(response);
    }

    @Override
    public void onEventStream(SdkPublisher<ConverseStreamOutput> publisher) {
        SdkPublisher<ConverseStreamOutput> tappedPublisher = publisher.map(new Function<ConverseStreamOutput, ConverseStreamOutput>() {
            @Override
            public ConverseStreamOutput apply(ConverseStreamOutput output) {
                if (!firstEventSeen) {
                    firstEventSeen = true;
                    timeToFirstToken = System.currentTimeMillis() - startTime;
                }
                modelResponse.apply(output);
                return output;
            }
        });
        delegate.onEventStream(tappedPublisher);
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        try {
            delegate.exceptionOccurred(throwable);
        } finally {
            modelResponse.markStreamError();
            finish();
        }
    }

    @Override
    public void complete() {
        try {
            delegate.complete();
        } finally {
            finish();
        }
    }

    private void finish() {
        try {
            ModelInvocation converseModelInvocation = new ConverseModelInvocation(linkingMetadata, userAttributes, converseStreamRequest,
                    modelResponse, timeToFirstToken);
            converseModelInvocation.recordLlmEventsAsync(startTime, token);
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, implementationTitle);
        } finally {
            if (segment != null) {
                segment.endAsync();
            }
        }
    }
}
