/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.test.marker.Java8IncompatibleTest;
import com.nr.instrumentation.graphql.helper.GraphQLTestHelper;
import com.nr.instrumentation.graphql.helper.PrivateApiStub;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.schema.GraphQLNonNull;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category({ Java8IncompatibleTest.class })
public class GraphQLSpanUtilTest {

    private static final List<Definition> NO_DEFINITIONS = Collections.emptyList();

    private PrivateApiStub privateApiStub;
    private PrivateApi privateApi;

    private static Stream<Arguments> providerForTestEdges() {
        return Stream.of(
                Arguments.of(null, null, "Unavailable", "<anonymous>", ""),
                Arguments.of(null, "{ hello }", "Unavailable", "<anonymous>", "{ hello }"),
                Arguments.of(new Document(NO_DEFINITIONS), "", "Unavailable", "<anonymous>", ""),
                Arguments.of(new Document(NO_DEFINITIONS), null, "Unavailable", "<anonymous>", "")
        );
    }

    @BeforeEach
    public void beforeEachTest() {
        privateApi = AgentBridge.privateApi;
        privateApiStub = new PrivateApiStub();
        AgentBridge.privateApi = privateApiStub;
    }

    @AfterEach
    public void afterEachTest() {
        AgentBridge.privateApi = privateApi;
    }

    @ParameterizedTest
    @CsvSource(value = {
            "query simple { libraries },QUERY,simple",
            "query { libraries },QUERY,<anonymous>",
            "{ hello },QUERY,<anonymous>",
            "mutation { data },MUTATION,<anonymous>",
            "mutation bob { data },MUTATION,bob"
    })
    public void testSetOperationAttributes(String query, String expectedType, String expectedName) {
        Document document = GraphQLTestHelper.parseDocumentFromText(query);
        GraphQLSpanUtil.setOperationAttributes(document, query);

        assertEquals(expectedType, privateApiStub.getTracerParameterFor("graphql.operation.type"));
        assertEquals(expectedName, privateApiStub.getTracerParameterFor("graphql.operation.name"));
        assertEquals(query, privateApiStub.getTracerParameterFor("graphql.operation.query"));
    }

    @ParameterizedTest
    @MethodSource("providerForTestEdges")
    public void testSetOperationAttributesEdgeCases(Document document, String query, String expectedType, String expectedName, String expectedQuery) {
        GraphQLSpanUtil.setOperationAttributes(document, query);

        assertEquals(expectedType, privateApiStub.getTracerParameterFor("graphql.operation.type"));
        assertEquals(expectedName, privateApiStub.getTracerParameterFor("graphql.operation.name"));
        assertEquals(expectedQuery, privateApiStub.getTracerParameterFor("graphql.operation.query"));
    }

    // This test verifies https://issues.newrelic.com/browse/NEWRELIC-4936 no longer exists
    @Test
    public void testInvalidClassExceptionFix() {
        // given execution parameters with execution step info type that is NOT GraphQLNamedSchemaElement
        String expectedFieldPath = "fieldPath";
        String expectedFieldName = "fieldName";
        ExecutionStrategyParameters parameters =
                mockExecutionStrategyParametersForInvalidClassCastExceptionTest(expectedFieldPath, expectedFieldName);

        // when (this had ClassCastException)
        GraphQLSpanUtil.setResolverAttributes(parameters);

        // then tracer parameters set correctly without exception
        assertEquals(expectedFieldPath, privateApiStub.getTracerParameterFor("graphql.field.path"));
        assertEquals(expectedFieldName, privateApiStub.getTracerParameterFor("graphql.field.name"));

        // and 'graphql.field.parentType' is not set
        assertNull(privateApiStub.getTracerParameterFor("graphql.field.parentType"),
                "'graphql.field.parentType' is not required and should be null here");
    }

    private static ExecutionStrategyParameters mockExecutionStrategyParametersForInvalidClassCastExceptionTest(String graphqlFieldPath,
            String graphqlFieldName) {
        ExecutionStrategyParameters parameters = mock(ExecutionStrategyParameters.class);
        ResultPath resultPath = mock(ResultPath.class);
        MergedField mergedField = mock(MergedField.class);
        ExecutionStepInfo executionStepInfo = mock(ExecutionStepInfo.class);
        GraphQLNonNull notGraphQLNamedSchemaElement = mock(GraphQLNonNull.class);

        when(resultPath.getSegmentName()).thenReturn(graphqlFieldPath);
        when(mergedField.getName()).thenReturn(graphqlFieldName);
        when(parameters.getPath()).thenReturn(resultPath);
        when(parameters.getField()).thenReturn(mergedField);
        when(parameters.getExecutionStepInfo()).thenReturn(executionStepInfo);
        when(executionStepInfo.getType()).thenReturn(notGraphQLNamedSchemaElement);

        return parameters;
    }
}
