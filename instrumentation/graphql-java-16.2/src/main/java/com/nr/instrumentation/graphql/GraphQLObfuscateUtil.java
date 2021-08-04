package com.nr.instrumentation.graphql;

import graphql.language.*;

import java.util.List;

public class GraphQLObfuscateUtil {

    private static final String OBFUSCATION = "(***)";
    private static final String PREFIX_FRAGMENT = "... on ";
    private static final String OBFUSCATION_ISSUE = "Issue with obfuscating query";

    public static String getObfuscatedQuery(Document document) {
        StringBuilder queryBuilder = new StringBuilder();
        //How is it possible to get a list of definitions?
        OperationDefinition operationDefinition = GraphQLTransactionName.getFirstOperationDefinitionFrom(document);
        if(operationDefinition != null){
            operationDefinition = (OperationDefinition) document.getDefinitions().get(0);
            makeOperationAndNameString(queryBuilder, operationDefinition);
            List<Node> fields = operationDefinition.getSelectionSet().getChildren();
            return buildGraph(queryBuilder, fields, 1).append("\n").append("}").toString();
        }
        return OBFUSCATION_ISSUE;
    }

    private static void makeOperationAndNameString(StringBuilder queryBuilder, OperationDefinition operationDefinition) {
        queryBuilder.append(operationDefinition.getOperation().name());
        queryBuilder.append(" ");
        queryBuilder.append(operationDefinition.getName() == null ? "" : operationDefinition.getName());
        queryBuilder.append("{");
    }

    private static StringBuilder buildGraph(StringBuilder builder, List<Node> fields, int queryLayer) {
        String indent = new String(new char[queryLayer * 2]).replace("\0", " ");
        for (Node field : fields) {
            NodeChildrenContainer children = field.getNamedChildren();
            SelectionSet selectionSet = children.getChildOrNull("selectionSet");
            //base case
            if (selectionSet == null) {
                builder.append("\n").append(indent);
                makeString(builder, field);
            } else {
                builder.append("\n").append(indent);
                makeString(builder, field);
                builder.append("{");
                //recursion
                buildGraph(builder, selectionSet.getChildren(), ++queryLayer);
                builder.append("\n").append(indent);
                builder.append("}");
            }
        }
        return builder;
    }

    private static void makeString(StringBuilder builder, Node field) {
        if (field instanceof Field) {
            Field castField = (Field) field;
            makeFieldString(builder, castField);
        }
        if (field instanceof InlineFragment) {
            InlineFragment fragment = (InlineFragment) field;
            makeFragmentString(builder, fragment);
        }
    }

    private static void makeFieldString(StringBuilder builder, Field field) {
        builder.append(getFieldAlias(field))
                .append(getFieldName(field))
                .append(obfuscateArguments(field));
    }

    private static void makeFragmentString(StringBuilder builder, InlineFragment fragment) {
        builder.append(PREFIX_FRAGMENT).append(getFragmentName(fragment));
    }

    private static String obfuscateArguments(Field field) {
        return field.getArguments().isEmpty() ? "" : OBFUSCATION;
    }

    private static String getFieldName(Field field){
        return field.getName() != null ? field.getName() : "";
    }

    private static String getFragmentName(InlineFragment fragment){
        TypeName typeCondition = fragment.getTypeCondition();
        if(typeCondition != null) {
            return typeCondition.getName();
        }
        return "";
    }

    private static String getFieldAlias(Field field){
        return field.getAlias() != null ? field.getAlias()+": " : "";
    }
}


