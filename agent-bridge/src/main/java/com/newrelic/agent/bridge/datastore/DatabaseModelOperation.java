package com.newrelic.agent.bridge.datastore;

public class DatabaseModelOperation {
    private final String model;
    private final String operation;

    public DatabaseModelOperation(String model, String operation) {
        this.model = model;
        this.operation = operation;
    }

    public String getModel() {
        return model;
    }

    public String getOperation() {
        return operation;
    }
}
