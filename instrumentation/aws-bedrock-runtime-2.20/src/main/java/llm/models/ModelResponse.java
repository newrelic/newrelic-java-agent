package llm.models;

public interface ModelResponse {
    String COMPLETION = "completion";
    String EMBEDDING = "embedding";

    String getResponseMessage();

    String getStopReason();

    int getInputTokenCount();

    int getOutputTokenCount();

    int getTotalTokenCount();

    String getAmznRequestId();

    String getOperationType();

    String getLlmChatCompletionSummaryId();

    String getLlmEmbeddingId();

    boolean isErrorResponse();

    int getStatusCode();

    String getStatusText();
}
