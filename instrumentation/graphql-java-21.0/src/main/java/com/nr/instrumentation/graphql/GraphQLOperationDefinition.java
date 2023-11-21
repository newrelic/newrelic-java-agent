/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.language.OperationDefinition;

import java.util.List;

public class GraphQLOperationDefinition {
    private final static String DEFAULT_OPERATION_DEFINITION_NAME = "<anonymous>";
    private final static String DEFAULT_OPERATION_NAME = "";

    // Multiple operations are supported for transaction name only
    // The underlying library does not seem to support multiple operations at time of this instrumentation
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
