package com.nr.instrumentation.graphql;

import com.newrelic.api.agent.NewRelic;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphQLInstrumentationUtil {
    private static final String METRIC_COUNT = "Custom/GraphQL/CallCount/Operations/%s";
    private static final String CATEGORY = "GraphQL";
    private static final String GRAPHQL_FIELDS_PARAM = "graphQL.fields";
    private static final String GRAPHQL_VARIABLES_PARAM = "variables.%s";
    public static final String QUERY_PARAM = "query";
    public static final String OPERATION_NAME_PARAM = "operationName";
    public static final int DEFAULT_SECURE_VALUE_ELISION_KEEP_CHAR_COUNT = 4;

    private static final List<String> TOP_LEVEL_FIELDS = Arrays.asList("actor", "account", "currentUser", "user", "docs", "nrPlatform");

    private static final boolean noticeErrors = true;
    private static final boolean elideSecureValues = true;
    private static final Function<SecureValue, Integer> secureValueElisionOriginalCharCountProvider = secureValue -> DEFAULT_SECURE_VALUE_ELISION_KEEP_CHAR_COUNT;

    public static ExecutionContext instrumentExecutionContext(
            ExecutionContext executionContext
    ) {
        if (executionContext == null) { return null; }

        List<String> fields = getFields(executionContext);

        changeTransactionName(executionContext, fields);

        NewRelic.addCustomParameter(GRAPHQL_FIELDS_PARAM, String.join("|", fields));
        fields.forEach((field) -> NewRelic.incrementCounter(String.format(METRIC_COUNT, field)));

        Map<String, Object> variables = executionContext.getVariables();
        if (elideSecureValues) {
            variables = sanitizeSecureValueVariables(variables);
        }
        variables.forEach((key, value) ->
                NewRelic.addCustomParameter(String.format(GRAPHQL_VARIABLES_PARAM, key), Objects.toString(value))
        );
        return executionContext;
    }

    private static Map<String, Object> sanitizeSecureValueVariables(Map<String, Object> variables) {
        HashMap<String, Object> sanitizedVariables = new HashMap<>(variables.size());
        variables.forEach((name, value) -> {
            if (value instanceof SecureValue) {
                SecureValue secureValue = ((SecureValue) value);
                String elideValue = secureValue.getElidedValue(secureValueElisionOriginalCharCountProvider.apply(secureValue));
                sanitizedVariables.put(name, elideValue);
            } else {
                sanitizedVariables.put(name, value);
            }
        });
        return sanitizedVariables;
    }

    public static CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult,
                                                                        ExecutionInput input) {
        if (executionResult == null) { return CompletableFuture.completedFuture(null); }

        noticeExpectedErrors(executionResult);

        NewRelic.addCustomParameter(QUERY_PARAM, input.getQuery());
        NewRelic.addCustomParameter(OPERATION_NAME_PARAM, input.getOperationName());

        return CompletableFuture.completedFuture(executionResult);
    }

    private static List<String> getFields(ExecutionContext executionContext) {
        return extractFields(executionContext.getOperationDefinition().getSelectionSet())
                .flatMap(GraphQLInstrumentationUtil::convertFields)
                .sorted()
                .collect(Collectors.toList());
    }

    private static Stream<String> convertFields(Field topField) {
        if (!TOP_LEVEL_FIELDS.contains(topField.getName())) {
            return Stream.of(topField.getName());
        }

        return extractFields(topField.getSelectionSet())
                .filter(subField -> !subField.getName().equals("__typename"))
                .map(subField -> topField.getName() + "." + subField.getName());
    }

    private static Stream<Field> extractFields(SelectionSet selectionSet) {
        return selectionSet.getSelections()
                .stream()
                .map(GraphQLInstrumentationUtil::convertToSelectionOrNull)
                .filter(Objects::nonNull);
    }

    private static Field convertToSelectionOrNull(Selection selection) {
        if (selection instanceof Field) {
            return (Field) selection;
        }

        return null;
    }

    private static void changeTransactionName(ExecutionContext executionContext, List<String> fields) {
        NewRelic.setTransactionName(
                CATEGORY,
                executionContext.getOperationDefinition().getOperation().toString() + "/" + String.join("::", fields)
        );
    }

    private static void noticeExpectedErrors(ExecutionResult executionResult) {
        if (!noticeErrors || executionResult.getErrors() == null) {
            return;
        }

        executionResult.getErrors().stream()
                .filter(error -> !(error instanceof ExceptionWhileDataFetching))
                .forEach(error -> NewRelic.noticeError(error.toString(), true));
    }
}
