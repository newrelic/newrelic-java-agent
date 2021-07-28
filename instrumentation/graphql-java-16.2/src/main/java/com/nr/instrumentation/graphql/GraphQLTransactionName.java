package com.nr.instrumentation.graphql;

import graphql.language.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GraphQLTransactionName {

    private final static String TYPENAME = "__typename";
    private final static String ID = "id";

    public static String from(Document document) {
        OperationDefinition operationDefinition = (OperationDefinition) document.getDefinitions().get(0);
        String name = operationDefinition.getName();
        String operation = operationDefinition.getOperation().name();
        if(name == null) {
            name = "<anonymous>";
        }
        StringBuilder sb = new StringBuilder("/")
                .append(operation)
                .append("/")
                .append(name)
                .append("/");

        SelectionSet selectionSet = operationDefinition.getSelectionSet();
        Selection selection = firstAndOnlyNonFederatedNamedNode(selectionSet);
        List<Selection> selections = new ArrayList<>();
        while(selection != null) {
            selections.add(selection);
            selection = nextNonFederatedNamedNode(selection);
        }
        sb.append(pathSuffixFrom(selections));
        return sb.toString();
    }

    private static String pathSuffixFrom(List<Selection> selections) {
        if(selections == null || selections.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(getName(selections.get(0)));
        int length = selections.size();
        for (int i = 1; i < length; i++) {
            Selection selection = selections.get(i);
            if(selection instanceof Field) {
                sb.append(".");
                sb.append(getName(selection));
            }
            else if(selection instanceof InlineFragment) {
                sb.append("<");
                sb.append(getName(selection));
                sb.append(">");
            }
        }
        return sb.toString();
    }

    private static Selection firstAndOnlyNonFederatedNamedNode(SelectionSet selectionSet) {
        if(selectionSet == null) {
            return null;
        }
        List<Selection> selections = selectionSet.getSelections();
        if(selections == null || selections.isEmpty()) {
            return null;
        }
        List<Selection> selection = selections.stream()
                .filter(namedNode -> notFederatedFieldName(getName(namedNode)))
                .collect(Collectors.toList());
        return selection.size() == 1 ? selection.get(0) : null;
    }

    private static String getName(Selection selection) {
        if(selection instanceof Field) {
            return ((Field) selection).getName();
        }
        if(selection instanceof InlineFragment) {
            return ((InlineFragment) selection).getTypeCondition().getName();
        }
        return null;
    }

    private static Selection nextNonFederatedNamedNode(Selection selection) {
        if(!(selection instanceof SelectionSetContainer)) {
            return null;
        }
        SelectionSet selectionSet = ((SelectionSetContainer<?>) selection).getSelectionSet();
        return firstAndOnlyNonFederatedNamedNode(selectionSet);
    }

    private static boolean notFederatedFieldName(String fieldName) {
        return !(TYPENAME.equals(fieldName) || ID.equals(fieldName));
    }

}
