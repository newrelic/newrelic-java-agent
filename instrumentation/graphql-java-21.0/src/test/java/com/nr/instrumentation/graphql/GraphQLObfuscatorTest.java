/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import com.newrelic.test.marker.Java8IncompatibleTest;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static com.nr.instrumentation.graphql.helper.GraphQLTestHelper.readText;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Category({ Java8IncompatibleTest.class })
public class GraphQLObfuscatorTest {

    private final static String OBFUSCATE_DATA_DIR = "obfuscateQueryTestData";

    @ParameterizedTest
    @CsvFileSource(resources = "/obfuscateQueryTestData/obfuscate-query-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testObfuscateQuery(String queryToObfuscateFilename, String expectedObfuscatedQueryFilename) {
        //setup
        queryToObfuscateFilename = queryToObfuscateFilename.trim();
        expectedObfuscatedQueryFilename = expectedObfuscatedQueryFilename.trim();
        String expectedObfuscatedResult = readText(OBFUSCATE_DATA_DIR, expectedObfuscatedQueryFilename);

        //given
        String query = readText(OBFUSCATE_DATA_DIR, queryToObfuscateFilename);

        //when
        String obfuscatedQuery = GraphQLObfuscator.obfuscate(query);

        //then
        assertEquals(expectedObfuscatedResult, obfuscatedQuery);
    }
}
