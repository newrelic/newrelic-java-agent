package com.nr.instrumentation.graphql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static com.nr.instrumentation.graphql.helper.GraphQLTestHelper.readText;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphQLObfuscatorTest {

    private final static String OBFUSCATE_DATA_DIR = "obfuscateQueryTestData";

    @ParameterizedTest
    @CsvFileSource(resources = "/obfuscateQueryTestData/obfuscate-query-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testObfuscateQuery(String queryToObfuscateFile, String expectedObfuscatedQueryFile) {
        //setup
        queryToObfuscateFile = queryToObfuscateFile.trim();
        expectedObfuscatedQueryFile = expectedObfuscatedQueryFile.trim();
        String expectedObfuscatedResult = readText(OBFUSCATE_DATA_DIR, expectedObfuscatedQueryFile);//readText(expectedObfuscatedQueryFile);

        //given
        String query = readText(OBFUSCATE_DATA_DIR, queryToObfuscateFile);

        //when
        String obfuscatedQuery = GraphQLObfuscator.obfuscate(query);
        assertEquals(expectedObfuscatedResult, obfuscatedQuery);
    }
}
