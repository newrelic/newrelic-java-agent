/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static llm.models.ModelInvocation.incrementInstrumentedSupportabilityMetric;
import static llm.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime asynchronously.
 */
// TODO switch back to instrumenting the BedrockRuntimeAsyncClient interface instead of this implementation class
@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.bedrockruntime.DefaultBedrockRuntimeAsyncClient")
final class DefaultBedrockRuntimeAsyncClient_Instrumentation {
//    private static final Logger log = LoggerFactory.getLogger(DefaultBedrockRuntimeAsyncClient.class);
//
//    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
//            .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final BedrockRuntimeServiceClientConfiguration serviceClientConfiguration;

    private final Executor executor;

    protected DefaultBedrockRuntimeAsyncClient_Instrumentation(BedrockRuntimeServiceClientConfiguration serviceClientConfiguration,
            SdkClientConfiguration clientConfiguration) {
        this.clientHandler = Weaver.callOriginal();
        this.clientConfiguration = Weaver.callOriginal();
        this.serviceClientConfiguration = Weaver.callOriginal();
        this.protocolFactory = Weaver.callOriginal();
        this.executor = Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<InvokeModelResponse> invokeModel(InvokeModelRequest invokeModelRequest) {
        long startTime = System.currentTimeMillis();
        // TODO name "Llm/" + operationType + "/Bedrock/InvokeModelAsync" ????
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("LLM", "InvokeModelAsync");
        CompletableFuture<InvokeModelResponse> invokeModelResponseFuture = Weaver.callOriginal();

        incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

        // this should never happen, but protecting against bad implementations
        if (invokeModelResponseFuture == null) {
            segment.end();
        } else {
            invokeModelResponseFuture.whenComplete((invokeModelResponse, throwable) -> {
                try {
                    // TODO do all the stuff
                    segment.end();
                } catch (Throwable t) {
                    AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                }
            });
        }

        return invokeModelResponseFuture;

    }

}
