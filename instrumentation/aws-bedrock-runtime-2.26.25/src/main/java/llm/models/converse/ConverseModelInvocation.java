package llm.models.converse;

import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Segment;
import llm.models.ModelInvocation;
import llm.models.ModelRequest;
import llm.models.ModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Collections;
import java.util.Map;

public class ConverseModelInvocation implements ModelInvocation {
    Map<String, String> linkingMetadata;
    Map<String, Object> userAttributes;
    ModelRequest modelRequest;
    ModelResponse modelResponse;

    public ConverseModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes, ConverseRequest converseRequest,
            ConverseResponse converseResponse) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new ConverseModelRequest(converseRequest);
        this.modelResponse = new ConverseModelResponse(converseResponse);
    }

    @Override
    public void setTracedMethodName(Transaction txn, String functionName) {

    }

    @Override
    public void setSegmentName(Segment segment, String functionName) {

    }

    @Override
    public void recordLlmEmbeddingEvent(long startTime, int index) {

    }

    @Override
    public void recordLlmChatCompletionSummaryEvent(long startTime, int numberOfMessages) {

    }

    @Override
    public void recordLlmChatCompletionMessageEvent(int sequence, String message, boolean isUser) {

    }

    @Override
    public void recordLlmEvents(long startTime) {

    }

    @Override
    public void recordLlmEventsAsync(long startTime, Token token) {

    }

    @Override
    public void reportLlmError() {

    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public ModelRequest getModelRequest() {
        return null;
    }

    @Override
    public ModelResponse getModelResponse() {
        return null;
    }
}
