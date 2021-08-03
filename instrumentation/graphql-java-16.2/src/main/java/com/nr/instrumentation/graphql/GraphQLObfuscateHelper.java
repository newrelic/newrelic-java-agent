package com.nr.instrumentation.graphql;

import graphql.language.*;

import java.util.List;

public class GraphQLObfuscateHelper {
    public static String obfuscate(Document document) {
        StringBuilder queryBuilder = new StringBuilder();

        //document.getDefinitions().get(0) - the OperationDefinition.
        //How is it possible to get a list of definitions??? Research this.
        OperationDefinition operationDefinition = null;
        if(!document.getDefinitions().isEmpty()){
            operationDefinition = (OperationDefinition) document.getDefinitions().get(0);
            queryBuilder.append(operationDefinition.getOperation().name());
            queryBuilder.append(" ");
            queryBuilder.append(operationDefinition.getName() == null ? "" : operationDefinition.getName());
            queryBuilder.append("{");

            //At this point of the first layer, the structure repeats into the layers.
            //List<Fields -> Field -> SelectionSet -> List<Fields> -> Field -> SelectionSet
            List<Node> fields = operationDefinition.getSelectionSet().getChildren();
            return buildGraph(queryBuilder, fields, 1).append("\n").append("}").toString();
        }
        return "no document definition found";
    }

    private static StringBuilder buildGraph(StringBuilder builder, List<Node> fields, int queryLayer) {
        String indent = new String(new char[queryLayer * 2]).replace("\0", " ");
        for (Node field : fields) {
            Field castField = (Field) field;
            SelectionSet selectionSet = castField.getSelectionSet();
            if (selectionSet == null) {
                builder.append("\n");
                builder.append(indent);

                builder.append(castField.getName());
            } else {
                builder.append("\n");
                builder.append(indent);
                builder.append(castField.getName());
                builder.append("{");
                buildGraph(builder, selectionSet.getChildren(), ++queryLayer);
                builder.append("\n");
                builder.append(indent);
                builder.append("}");
            }
        }
        return builder;
    }
}


