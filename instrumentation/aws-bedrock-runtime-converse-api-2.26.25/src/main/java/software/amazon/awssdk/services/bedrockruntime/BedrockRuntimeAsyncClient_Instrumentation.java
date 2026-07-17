/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.converse.models.ModelInvocation;
import llm.converse.models.ModelResponse;
import llm.converse.models.converse.ConverseModelInvocation;
import llm.converse.models.converse.ConverseStreamResponseHandlerWrapper;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringStreamingEnabled;
import static llm.converse.vendor.Vendor.BEDROCK;
import static llm.converse.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime asynchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient")
public abstract class BedrockRuntimeAsyncClient_Instrumentation {

    @Trace
    public CompletableFuture<ConverseResponse> converse(ConverseRequest converseRequest) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<ConverseResponse> converseResponseFuture = Weaver.callOriginal();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                // Segment will be renamed later when the response is available
                Segment segment = txn.startSegment("");
                // Set llm = true agent attribute, this is required on transaction events
                ModelInvocation.setLlmTrueAgentAttribute(txn);

                // This should never happen, but protecting against bad implementations
                if (converseResponseFuture == null) {
                    segment.end();
                } else {
                    Map<String, Object> userAttributes = txn.getUserAttributes();
                    Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

                    Token token = txn.getToken();

                    // Instrumentation fails if the BiConsumer is replaced with a lambda
                    converseResponseFuture.whenComplete(new BiConsumer<ConverseResponse, Throwable>() {
                        @Override
                        public void accept(ConverseResponse converseResponse, Throwable throwable) {
                            try {
                                ModelInvocation converseModelInvocation = new ConverseModelInvocation(linkingMetadata, userAttributes, converseRequest,
                                        converseResponse);
                                // Set traced method name based on LLM operation from response
                                converseModelInvocation.setSegmentName(segment, "converse");
                                converseModelInvocation.recordLlmEventsAsync(startTime, token);

                                if (segment != null) {
                                    segment.endAsync();
                                }
                            } catch (Throwable t) {
                                if (segment != null) {
                                    segment.endAsync();
                                }
                                AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                            }
                        }
                    });
                }
            }
        }
        return converseResponseFuture;
    }

    @Trace
    public CompletableFuture<Void> converseStream(ConverseStreamRequest converseStreamRequest, ConverseStreamResponseHandler asyncResponseHandler) {
        long startTime = System.currentTimeMillis();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                Segment segment = txn.startSegment("");
                ModelInvocation.setLlmTrueAgentAttribute(txn);
                segment.setMetricName("Llm", ModelResponse.COMPLETION, BEDROCK, "converseStream");

                if (asyncResponseHandler == null) {
                    segment.end();
                } else if (isAiMonitoringStreamingEnabled()) {
                    Map<String, Object> userAttributes = txn.getUserAttributes();
                    Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
                    Token token = txn.getToken();

                    asyncResponseHandler = new ConverseStreamResponseHandlerWrapper(asyncResponseHandler, converseStreamRequest, linkingMetadata,
                            userAttributes, segment, token, startTime, Weaver.getImplementationTitle());
                } else {
                    segment.end();
                }
            }
        }

        return Weaver.callOriginal();
    }
}
