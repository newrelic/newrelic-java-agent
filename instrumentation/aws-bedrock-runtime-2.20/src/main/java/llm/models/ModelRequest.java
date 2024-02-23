package llm.models;

public interface ModelRequest {
    int getMaxTokensToSample();

    float getTemperature();

    String getRequestMessage();

    String getRole();

    String getInputText();

    String getModelId();
}
