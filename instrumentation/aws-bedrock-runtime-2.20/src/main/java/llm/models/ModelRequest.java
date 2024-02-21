package llm.models;

public interface ModelRequest {
    String getMaxTokensToSample();

    String getTemperature();

    String getRequestMessage();

    String getRole();

    String getInputText();

    String getModelId();
}
