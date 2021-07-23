package com.nr.instrumentation.graphql;

import graphql.language.*;

import java.util.ArrayList;
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
        List<String> names = new ArrayList<>();
        while(firstName != null) {
            names.add(firstName);
            selectionSet = nextSelectionSetFrom(selectionSet);
            firstName = firstName(selectionSet);
        }
        sb.append(String.join(".", names));
        return sb.toString();
    }

    private static SelectionSet nextSelectionSetFrom(SelectionSet selectionSet) {
        return ((Field) selectionSet.getSelections().get(0)).getSelectionSet();
    }

    private static String firstName(SelectionSet selectionSet) {
        if(selectionSet == null) {
            return null;
        }
        List<Selection> selections = selectionSet.getSelections();
        if(selections.size() == 1) {
            return ((Field) selectionSet.getSelections().get(0)).getName();
        }
        return null;
    }
}
