package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.models.ModelInvocation;
import llm.models.anthropic.claude.AnthropicClaudeModelInvocation;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static llm.models.SupportedModels.ANTHROPIC_CLAUDE;
import static llm.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime asynchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient")
public abstract class BedrockRuntimeAsyncClient_Instrumentation {

    @Trace
    public CompletableFuture<InvokeModelResponse> invokeModel(InvokeModelRequest invokeModelRequest) {
        // TODO check AIM config
        long startTime = System.currentTimeMillis();
        Transaction txn = AgentBridge.getAgent().getTransaction();
        // Segment will be named later when the response is available
        Segment segment = txn.startSegment("");

        CompletableFuture<InvokeModelResponse> invokeModelResponseFuture = Weaver.callOriginal();

        // Set llm = true agent attribute
        ModelInvocation.setLlmTrueAgentAttribute(txn);
        ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);
        Map<String, Object> userAttributes = txn.getUserAttributes();
        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

        // this should never happen, but protecting against bad implementations
        if (invokeModelResponseFuture == null) {
            segment.end();
        } else {
            invokeModelResponseFuture.whenComplete(new BiConsumer<InvokeModelResponse, Throwable>() {
                @Override
                public void accept(InvokeModelResponse invokeModelResponse, Throwable throwable) {
                    try {
                        // TODO check AIM config
                        String modelId = invokeModelRequest.modelId();
                        if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
                            ModelInvocation anthropicClaudeModelInvocation = new AnthropicClaudeModelInvocation(userAttributes, invokeModelRequest,
                                    invokeModelResponse);
                            // Set segment name based on LLM operation
                            anthropicClaudeModelInvocation.setSegmentName(segment, "invokeModel");
                            anthropicClaudeModelInvocation.recordLlmEvents(startTime, linkingMetadata);
                        }
                        segment.end();
                    } catch (Throwable t) {
                        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                    }
                }
            });
        }
        return invokeModelResponseFuture;
    }
}
