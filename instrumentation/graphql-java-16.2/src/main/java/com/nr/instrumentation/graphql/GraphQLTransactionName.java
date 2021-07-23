package com.nr.instrumentation.graphql;

import graphql.language.*;

import java.util.List;

public class GraphQLTransactionName {
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
        String firstName = firstName(selectionSet);
        if(firstName != null) {
            sb.append(firstName(selectionSet));
            firstName = firstName(((Field) selectionSet.getSelections().get(0)).getSelectionSet());
            if(firstName != null) {
                sb.append(".");
                sb.append(firstName);
            }
        }
        return sb.toString();
    }

    private static String firstName(SelectionSet selectionSet) {
        if(selectionSet == null) {
            return null;
        }
        List<Selection> selections = selectionSet.getSelections();
        if(!selections.isEmpty()) {
            return ((Field) selectionSet.getSelections().get(0)).getName();
        }
        return null;
    }
}
