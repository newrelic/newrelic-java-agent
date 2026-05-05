/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.embedding;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.embeddings.models.ModelInvocation;
import llm.embeddings.models.springai.SpringAiModelInvocation;

import java.util.Map;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static llm.embeddings.vendor.Vendor.VENDOR_VERSION;

@Weave(type = MatchType.Interface, originalName = "org.springframework.ai.embedding.EmbeddingModel")
public abstract class EmbeddingModel_Instrumentation {

    @Trace
    public EmbeddingResponse call(EmbeddingRequest request) {
        long startTime = System.currentTimeMillis();
        EmbeddingResponse embeddingResponse = Weaver.callOriginal();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                // Set llm = true agent attribute, this is required on transaction events
                ModelInvocation.setLlmTrueAgentAttribute(txn);

                Map<String, Object> userAttributes = txn.getUserAttributes();
                Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

                // Create Spring AI model invocation
                ModelInvocation springAiInvocation = new SpringAiModelInvocation(
                        linkingMetadata, userAttributes, request, embeddingResponse);
                springAiInvocation.setTracedMethodName(txn, "call");
                springAiInvocation.recordLlmEvents(startTime);
            }
        }

        return embeddingResponse;
    }
}
