package com.nr.instrumentation.graphql;

import graphql.language.*;

import java.util.ArrayList;
import java.util.List;

public class GraphQLTransactionName {

    private static Selection<Field> selection;

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
        String firstName = firstAndOnlyNonFederatedFieldName(selectionSet);
        List<String> names = new ArrayList<>();
        while(firstName != null) {
            names.add(firstName);
            selectionSet = nextSelectionSetFrom(selectionSet);
            firstName = firstAndOnlyNonFederatedFieldName(selectionSet);
        }
        sb.append(String.join(".", names));
        return sb.toString();
    }

    private static SelectionSet nextSelectionSetFrom(SelectionSet selectionSet) {
        List<Selection> selections = selectionSet.getSelections();
        if(selections == null || selections.isEmpty()) {
            return null;
        }
        Selection selection = selections.get(0);
        if(selection instanceof Field) {
            return ((Field) selection).getSelectionSet();
        }
        if(selection instanceof InlineFragment) {
            return ((InlineFragment) selection).getSelectionSet();
        }
        return null;
    }

    private final static String TYPENAME = "__typename";
    private final static String ID = "id";

    private static boolean notFederatedFieldName(String fieldName) {
        return !(TYPENAME.equals(fieldName) || ID.equals(fieldName));
    }

    private static boolean isInlineFragment(String name) {
        return name != null && name.startsWith("<") && name.endsWith(">");
    }

    private static String firstAndOnlyNonFederatedFieldName(SelectionSet selectionSet) {
        if(selectionSet == null) {
            return null;
        }
        String name = null;
        for (Selection selection : selectionSet.getSelections()) {
            String nextFieldName = fieldNameFrom(selection);
            if(nextFieldName != null) {
                if(notFederatedFieldName(nextFieldName)) {
                    if(name != null) {
                        return null;
                    }
                    else {
                        name = nextFieldName;
                    }
                }
            }
        }
        return name;
    }

    private static String fieldNameFrom(Selection selection) {
        if(selection instanceof Field) {
            return ((Field) selection).getName();
        }
        if(selection instanceof InlineFragment) {
            return "<" + ((InlineFragment) selection).getTypeCondition().getName() + ">";
        }
        return null;
    }


}
