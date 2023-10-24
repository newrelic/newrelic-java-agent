/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.SelectionSetContainer;
import graphql.language.TypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.nr.instrumentation.graphql.Utils.isNullOrEmpty;

/**
 * Generates GraphQL transaction names based on details referenced in Node instrumentation.
 *
 * @see <a href="https://github.com/newrelic/newrelic-node-apollo-server-plugin/blob/main/docs/transactions.md">
 * NewRelic Node Apollo Server Plugin - Transactions
 * </a>
 * <p>
 * Batch queries are not supported by GraphQL Java implementation at this time
 * and transaction names for parse errors must be set elsewhere because this class
 * relies on the GraphQL Document that is the artifact of a successful parse.
 */
public class GraphQLTransactionName {

    private final static String DEFAULT_TRANSACTION_NAME = "";

    // federated field names to exclude from path calculations
    private final static String TYPENAME = "__typename";
    private final static String ID = "id";

    /**
     * Generates a transaction name based on a valid, parsed GraphQL Document
     *
     * @param document parsed GraphQL Document
     * @return a transaction name based on given document
     */
    public static String from(final Document document) {
        if (document == null) {
            return DEFAULT_TRANSACTION_NAME;
        }
        List<OperationDefinition> operationDefinitions = document.getDefinitionsOfType(OperationDefinition.class);
        if (isNullOrEmpty(operationDefinitions)) {
            return DEFAULT_TRANSACTION_NAME;
        }
        if (operationDefinitions.size() == 1) {
            return getTransactionNameFor(operationDefinitions.get(0));
        }
        return "/batch" + operationDefinitions.stream()
                .map(GraphQLTransactionName::getTransactionNameFor)
                .collect(Collectors.joining());
    }

    private static String getTransactionNameFor(OperationDefinition operationDefinition) {
        if (operationDefinition == null) {
            return DEFAULT_TRANSACTION_NAME;
        }
        return createBeginningOfTransactionNameFrom(operationDefinition) +
                createEndOfTransactionNameFrom(operationDefinition.getSelectionSet());
    }

    private static String createBeginningOfTransactionNameFrom(final OperationDefinition operationDefinition) {
        String operationType = GraphQLOperationDefinition.getOperationTypeFrom(operationDefinition);
        String operationName = GraphQLOperationDefinition.getOperationNameFrom(operationDefinition);
        return String.format("/%s/%s", operationType, operationName);
    }

    private static String createEndOfTransactionNameFrom(final SelectionSet selectionSet) {
        Selection selection = onlyNonFederatedSelectionOrNoneFrom(selectionSet);
        if (selection == null) {
            return "";
        }
        List<Selection> selections = new ArrayList<>();
        while (selection != null) {
            selections.add(selection);
            selection = nextNonFederatedSelectionChildFrom(selection);
        }
        return createPathSuffixFrom(selections);
    }

    private static String createPathSuffixFrom(final List<Selection> selections) {
        if (selections == null || selections.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("/").append(getNameFrom(selections.get(0)));
        int length = selections.size();
        // skip first element, it is already added without extra formatting
        for (int i = 1; i < length; i++) {
            sb.append(getFormattedNameFor(selections.get(i)));
        }
        return sb.toString();
    }

    private static String getFormattedNameFor(Selection selection) {
        if (selection instanceof Field) {
            return String.format(".%s", getNameFrom((Field) selection));
        }
        if (selection instanceof InlineFragment) {
            return String.format("<%s>", getNameFrom((InlineFragment) selection));
        }
        return "";
    }

    private static Selection onlyNonFederatedSelectionOrNoneFrom(final SelectionSet selectionSet) {
        if (selectionSet == null) {
            return null;
        }
        List<Selection> selections = selectionSet.getSelections();
        if (isNullOrEmpty(selections)) {
            return null;
        }
        List<Selection> selection = selections.stream()
                .filter(namedNode -> notFederatedFieldName(getNameFrom(namedNode)))
                .collect(Collectors.toList());
        // there can be only one, or we stop digging into query
        return selection.size() == 1 ? selection.get(0) : null;
    }

    private static String getNameFrom(final Selection selection) {
        if (selection instanceof Field) {
            return getNameFrom((Field) selection);
        }
        if (selection instanceof InlineFragment) {
            return getNameFrom((InlineFragment) selection);
        }
        // FragmentSpread also implements Selection but not sure how that might apply here
        return null;
    }

    private static String getNameFrom(final Field field) {
        return field.getName();
    }

    private static String getNameFrom(final InlineFragment inlineFragment) {
        TypeName typeCondition = inlineFragment.getTypeCondition();
        if (typeCondition != null) {
            return typeCondition.getName();
        }
        return "";
    }

    private static Selection nextNonFederatedSelectionChildFrom(final Selection selection) {
        if (!(selection instanceof SelectionSetContainer)) {
            return null;
        }
        SelectionSet selectionSet = ((SelectionSetContainer<?>) selection).getSelectionSet();
        return onlyNonFederatedSelectionOrNoneFrom(selectionSet);
    }

    private static boolean notFederatedFieldName(final String fieldName) {
        return !(TYPENAME.equals(fieldName) || ID.equals(fieldName));
    }
}
