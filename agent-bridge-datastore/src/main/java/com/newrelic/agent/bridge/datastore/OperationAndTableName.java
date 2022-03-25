package com.newrelic.agent.bridge.datastore;

public class OperationAndTableName {
    private final String operation;
    private final String tableName;

    public OperationAndTableName(String operation, String tableName) {
        this.operation = operation;
        this.tableName = tableName;
    }

    public String getOperation() {
        return operation;
    }

    public String getTableName() {
        return tableName;
    }
}
