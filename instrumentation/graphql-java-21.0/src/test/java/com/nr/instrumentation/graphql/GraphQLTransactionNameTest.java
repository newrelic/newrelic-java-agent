/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import com.newrelic.test.marker.Java8IncompatibleTest;
import graphql.language.Document;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static com.nr.instrumentation.graphql.helper.GraphQLTestHelper.parseDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Category({ Java8IncompatibleTest.class })
public class GraphQLTransactionNameTest {

    private final static String TEST_DATA_DIR = "transactionNameTestData";

    @ParameterizedTest
    @CsvFileSource(resources = "/transactionNameTestData/transaction-name-test-data.csv", delimiter = '|', numLinesToSkip = 2)
    public void testQuery(String testFileName, String expectedTransactionName) {
        //setup
        testFileName = testFileName.trim();
        expectedTransactionName = expectedTransactionName.trim();
        //given
        Document document = parseDocument(TEST_DATA_DIR, testFileName);
        //when
        String transactionName = GraphQLTransactionName.from(document);
        //then
        assertEquals(expectedTransactionName, transactionName);
    }
}
