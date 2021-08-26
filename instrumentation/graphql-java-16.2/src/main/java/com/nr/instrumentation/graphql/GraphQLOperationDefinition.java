package com.nr.instrumentation.graphql;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.OperationDefinition;

import java.util.List;
import java.util.Optional;

public class GraphQLOperationDefinition {
    private final static String DEFAULT_OPERATION_DEFINITION_NAME = "<anonymous>";
    private final static String DEFAULT_OPERATION_NAME = "";

    // TODO: What to do with multiple operations
    public static OperationDefinition firstFrom(final Document document) {
        List<OperationDefinition> operationDefinitions = document.getDefinitionsOfType(OperationDefinition.class);
        return operationDefinitions.isEmpty() ? null : operationDefinitions.get(0);
    }

    public static String getOperationNameFrom(final OperationDefinition operationDefinition) {
        return operationDefinition.getName() != null ? operationDefinition.getName() : DEFAULT_OPERATION_DEFINITION_NAME;
    }

    public static String getOperationTypeFrom(final OperationDefinition operationDefinition) {
        OperationDefinition.Operation operation = operationDefinition.getOperation();
        return operation != null ? operation.name() : DEFAULT_OPERATION_NAME;
    }
}
