/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.InvokeModelRequestWrapper;
import com.newrelic.utils.InvokeModelResponseWrapper;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.utils.BedrockRuntimeUtil.debugLoggingForDevelopment;
import static com.newrelic.utils.BedrockRuntimeUtil.incrementBedrockInstrumentedMetric;
import static com.newrelic.utils.BedrockRuntimeUtil.reportLlmChatCompletionMessageEvent;
import static com.newrelic.utils.BedrockRuntimeUtil.reportLlmChatCompletionSummaryEvent;
import static com.newrelic.utils.BedrockRuntimeUtil.reportLlmEmbeddingEvent;
import static com.newrelic.utils.BedrockRuntimeUtil.setLlmOperationMetricName;
import static com.newrelic.utils.InvokeModelResponseWrapper.COMPLETION;
import static com.newrelic.utils.InvokeModelResponseWrapper.EMBEDDING;

/**
 * Service client for accessing Amazon Bedrock Runtime.
 */
@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.bedrockruntime.DefaultBedrockRuntimeClient")
final class DefaultBedrockRuntimeClient_Instrumentation {
//    private static final Logger log = Logger.loggerFor(DefaultBedrockRuntimeClient.class);
//
//    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
//            .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final BedrockRuntimeServiceClientConfiguration serviceClientConfiguration;

    protected DefaultBedrockRuntimeClient_Instrumentation(BedrockRuntimeServiceClientConfiguration serviceClientConfiguration,
            SdkClientConfiguration clientConfiguration) {
        this.clientHandler = Weaver.callOriginal();
        this.clientConfiguration = Weaver.callOriginal();
        this.serviceClientConfiguration = Weaver.callOriginal();
        this.protocolFactory = Weaver.callOriginal();
    }

    @Trace
    public InvokeModelResponse invokeModel(InvokeModelRequest invokeModelRequest) {
        InvokeModelResponse invokeModelResponse = Weaver.callOriginal();

        incrementBedrockInstrumentedMetric();

        Transaction txn = NewRelic.getAgent().getTransaction();
        // TODO check AIM config
        if (txn != null && !(txn instanceof NoOpTransaction)) {
            debugLoggingForDevelopment(txn, invokeModelRequest, invokeModelResponse); // FIXME delete

            Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
            InvokeModelRequestWrapper requestWrapper = new InvokeModelRequestWrapper(invokeModelRequest);
            InvokeModelResponseWrapper responseWrapper = new InvokeModelResponseWrapper(invokeModelResponse);

            String operationType = responseWrapper.getOperationType();
            // Set traced method name based on LLM operation
            setLlmOperationMetricName(txn, operationType);

            // Report LLM events
            if (operationType.equals(COMPLETION)) {
                reportLlmChatCompletionMessageEvent(txn, linkingMetadata, requestWrapper, responseWrapper);
                reportLlmChatCompletionSummaryEvent(txn, linkingMetadata, requestWrapper, responseWrapper);
            } else if (operationType.equals(EMBEDDING)) {
                reportLlmEmbeddingEvent(txn, linkingMetadata, requestWrapper, responseWrapper);
            } else {
                NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unexpected operation type");
            }
        }

        return invokeModelResponse;
    }

}
