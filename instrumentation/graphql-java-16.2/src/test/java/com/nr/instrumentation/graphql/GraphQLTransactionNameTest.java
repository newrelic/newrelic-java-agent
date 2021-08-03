package com.nr.instrumentation.graphql;

import graphql.language.*;
import graphql.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphQLTransactionNameTest {

    private final static String TEST_DATA_DIR = "transactionNameTestData";

    private static Document parse(String filename) {
        return Parser.parse(readText(filename));
    }

    private static String readText(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + filename + ".gql")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBuildQuery() {
        //setup
        String testFileName = "buildQueryString";
        String expectedTransactionName = "who cares";
        testFileName = TEST_DATA_DIR + "/" + testFileName.trim();
        expectedTransactionName = expectedTransactionName.trim();
        //given
        Document document = parse(testFileName);

        //StringBuilder
        StringBuilder builder = new StringBuilder();
        SelectionSet set = (SelectionSet) document.getChildren().get(0).getChildren().get(0);

        //document.getDefinitions() - returns a list of definitions. In examples, I only see one definition. How cxan a query have multiple???

        //document.getDefinitions().get(0) - the OperationDefinition
        OperationDefinition opDef = (OperationDefinition) document.getDefinitions().get(0);
        builder.append(opDef.getOperation().name());
        builder.append(" ");
        builder.append(opDef.getName() == null ? "" : opDef.getName());
        builder.append("{");

        //opDef.getSelectionSet().getChildren() - List<Fields> being queried in first layer
        //At this point of the first layer, the structure is Field -> SelectionSet -> List<Fields>
        List<Node> fields = opDef.getSelectionSet().getChildren();
        String finalGraph = buildGraph(builder, fields, 1).append("\n").append("}").toString();
        System.out.println("");
    }

    private StringBuilder buildGraph(StringBuilder builder, List<Node> fields, int queryLayer) {
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

    @ParameterizedTest
    @CsvFileSource(resources = "/transactionNameTestData/transaction-name-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testQuery(String testFileName, String expectedTransactionName) {
        //setup
        testFileName = TEST_DATA_DIR + "/" + testFileName.trim();
        expectedTransactionName = expectedTransactionName.trim();
        //given
        Document document = parse(testFileName);
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals(expectedTransactionName, transactionName);
    }
}
