/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.converse.models.ModelInvocation;
import llm.converse.models.converse.ConverseModelInvocation;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

import java.util.Map;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static llm.converse.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime synchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient")
public abstract class BedrockRuntimeClient_Instrumentation {

    @Trace
    public ConverseResponse converse(ConverseRequest converseRequest) {
        long startTime = System.currentTimeMillis();
        ConverseResponse converseResponse = Weaver.callOriginal();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                // Set llm = true agent attribute, this is required on transaction events
                ModelInvocation.setLlmTrueAgentAttribute(txn);

                Map<String, Object> userAttributes = txn.getUserAttributes();
                Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
                ModelInvocation converseModelInvocation = new ConverseModelInvocation(linkingMetadata, userAttributes, converseRequest, converseResponse);

                // Set traced method name based on LLM operation from response
                converseModelInvocation.setTracedMethodName(txn, "converse");
                converseModelInvocation.recordLlmEvents(startTime);
            }
        }
        return converseResponse;
    }
}
