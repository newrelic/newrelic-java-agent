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
        NamedNode<?> namedNode = firstAndOnlyNonFederatedNamedNode(selectionSet);
        List<NamedNode<?>> namedNodes = new ArrayList<>();
        while(namedNode != null) {
            namedNodes.add(namedNode);
            namedNode = nextNonFederatedNamedNode(namedNode);
        }
        sb.append(namedNodes.stream().map(NamedNode::getName).collect(Collectors.joining(".")));
        return sb.toString();
    }

    private static NamedNode<?> firstAndOnlyNonFederatedNamedNode(SelectionSet selectionSet) {
        if(selectionSet == null) {
            return null;
        }
        List<Selection> selections = selectionSet.getSelections();
        if(selections == null || selections.isEmpty()) {
            return null;
        }
        List<NamedNode<?>> namedNodes = selections.stream()
                .filter(selection -> selection instanceof NamedNode)
                .map(selection -> (NamedNode<?>) selection)
                .filter(namedNode -> notFederatedFieldName(namedNode.getName()))
                .collect(Collectors.toList());
        return namedNodes.size() == 1 ? namedNodes.get(0) : null;
    }

    private static NamedNode<?> nextNonFederatedNamedNode(NamedNode<?> namedNode) {
        if(!(namedNode instanceof SelectionSetContainer)) {
            return null;
        }
        SelectionSet selectionSet = ((SelectionSetContainer<?>) namedNode).getSelectionSet();
        return firstAndOnlyNonFederatedNamedNode(selectionSet);
    }

    private static boolean notFederatedFieldName(String fieldName) {
        return !(TYPENAME.equals(fieldName) || ID.equals(fieldName));
    }

}
