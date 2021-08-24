package com.nr.instrumentation.graphql;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.OperationDefinition;

import java.util.List;
import java.util.Optional;

public class GraphQLOperationDefinition {
    private final static String DEFAULT_OPERATION_DEFINITION_NAME = "<anonymous>";
    private final static String DEFAULT_OPERATION_NAME = "";

    // At this point, not sure when we would have something different or more than one but to be safe
    public static OperationDefinition firstFrom(final Document document) {
        List<Definition> definitions = document.getDefinitions();
        if(definitions == null || definitions.isEmpty()) {
            return null;
        }
        Optional<Definition> definitionOptional = definitions.stream()
                .filter(d -> d instanceof OperationDefinition)
                .findFirst();
        return (OperationDefinition) definitionOptional.orElse(null);
    }

    public static String getOperationNameFrom(final OperationDefinition operationDefinition) {
        return operationDefinition.getName() != null ? operationDefinition.getName() : DEFAULT_OPERATION_DEFINITION_NAME;
    }

    public static String getOperationTypeFrom(final OperationDefinition operationDefinition) {
        OperationDefinition.Operation operation = operationDefinition.getOperation();
        return operation != null ? operation.name() : DEFAULT_OPERATION_NAME;
    }
}
