package com.nr.instrumentation.graphql;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.api.agent.Trace;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"graphql", "com.nr.instrumentation"}, configName = "distributed_tracing.yml")
public class GraphQL_InstrumentationTest {
    private static final long DEFAULT_TIMEOUT_IN_MILLIS = 10_000;
    private static final String TEST_ARG = "testArg";

    private static GraphQL graphQL;

    @BeforeClass
    public static void initialize() {
        String schema = "type Query{hello("+ TEST_ARG +": String): String}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("hello",
                        new StaticDataFetcher("world")))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    @AfterEach
    public void cleanUp() {
        InstrumentationTestRunner.getIntrospector().clear();
    }

    @Test
    public void queryWithNoArg() {
        //given
        String query = "{hello}";
        //when
        trace(createRunnable(query));
        //then
        assertRequestNoArg("QUERY/<anonymous>/hello", "{hello}");
    }

    @Test
    public void queryWithArg() {
        //given
        String query = "{hello ("+ TEST_ARG +": \"fo)o\")}";
        //when
        trace(createRunnable(query));
        //then
        assertRequestWithArg("QUERY/<anonymous>/hello", "{hello ("+ TEST_ARG +": ***)}");
    }

    @Test
    public void parsingException() {
        //given
        String query = "cause a parse error";
        //when
        trace(createRunnable(query));
        //then
        String expectedErrorMessage = "Invalid Syntax : offending token 'cause' at line 1 column 1";
        assertErrorOperation("post/*", "ParseAndValidate/parse", "graphql.parser.InvalidSyntaxException", expectedErrorMessage, true);
    }

    @Test
    public void validationException() {
        //given
        String query = "{noSuchField}";
        //when
        trace(createRunnable(query));
        //then
        String expectedErrorMessage = "Validation error of type FieldUndefined: Field 'noSuchField' in type 'Query' is undefined @ 'noSuchField'";
        assertErrorOperation("QUERY/<anonymous>/noSuchField",
                "ParseAndValidate/validate", "graphql.GraphqlErrorException",  expectedErrorMessage, false);
    }

    @Test
    public void resolverException() {
        //given
        String query = "{hello}";

        //when
        trace(createRunnable(query, graphWithResolverException()));
        //then
        String expectedErrorMessage = "Exception while fetching data (/hello) : null";
        assertErrorOperation("QUERY/<anonymous>/hello", "GraphQL/resolve/hello", "graphql.GraphqlErrorException", expectedErrorMessage, false);
    }

    @Trace(dispatcher = true)
    private void trace(Runnable runnable) {
        runnable.run();
    }

    private Runnable createRunnable(final String query){
        return () -> graphQL.execute(query);
    }

    private Runnable createRunnable(final String query, GraphQL graphql){
        return () -> graphql.execute(query);
    }

    private GraphQL graphWithResolverException() {
        String schema = "type Query{hello("+ TEST_ARG +": String): String}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("hello", environment -> {
                            throw new RuntimeException();
                        })
                )
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    private void assertOneTxFinishedWithExpectedName(Introspector introspector, String expectedTransactionSuffix, boolean isParseError){
        assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_IN_MILLIS));
        String txName = introspector.getTransactionNames().iterator().next();
        if(!isParseError) {
            assertEquals("Transaction name is incorrect",
                "OtherTransaction/GraphQL/" + expectedTransactionSuffix, txName);
        } else {
            assertEquals("Transaction name is incorrect",
                    "OtherTransaction/" + expectedTransactionSuffix, txName);
        }
    }

    private boolean attributeValueOnSpan(Introspector introspector, String spanName, String attribute, String value) {
        List<SpanEvent> spanEvents = introspector.getSpanEvents().stream()
                .filter(spanEvent -> spanEvent.getName().contains(spanName))
                .collect(Collectors.toList());
        Assert.assertEquals(1, spanEvents.size());
        Assert.assertNotNull(spanEvents.get(0).getAgentAttributes().get(attribute));
        Assert.assertEquals(value, spanEvents.get(0).getAgentAttributes().get(attribute));
        return true;
    }

    private boolean scopedAndUnscopedMetrics(Introspector introspector, String metricPrefix) {
        boolean scoped = introspector.getMetricsForTransaction(introspector.getTransactionNames().iterator().next())
                .keySet().stream().anyMatch(s -> s.contains(metricPrefix));
        boolean unscoped = introspector.getUnscopedMetrics().keySet().stream().anyMatch(s -> s.contains(metricPrefix));
        return scoped && unscoped;
    }

    private boolean expectedMetrics(Introspector introspector) {
        assertTrue(scopedAndUnscopedMetrics(introspector,  "GraphQL/operation/"));
        assertTrue(scopedAndUnscopedMetrics(introspector,  "GraphQL/resolve/"));
        return true;
    }

    private boolean expectedResolverAttributes(Introspector introspector) {
        assertTrue(attributeValueOnSpan(introspector, "GraphQL/resolve", "graphql.field.parentType", "Query"));
        assertTrue(attributeValueOnSpan(introspector, "GraphQL/resolve", "graphql.field.name", "hello"));
        assertTrue(attributeValueOnSpan(introspector, "GraphQL/resolve", "graphql.field.path", "hello"));
        return true;
    }

    private boolean expectedOperationAttributes(Introspector introspector, String spanName ) {
        assertTrue(attributeValueOnSpan(introspector, spanName, "graphql.operation.name", "<anonymous>"));
        assertTrue(attributeValueOnSpan(introspector, spanName, "graphql.operation.type", "QUERY"));
        return true;
    }

    private void assertRequestNoArg(String expectedTransactionSuffix, String expectedQueryAttribute) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertOneTxFinishedWithExpectedName(introspector, expectedTransactionSuffix, false);
        assertTrue(attributeValueOnSpan(introspector, expectedTransactionSuffix, "graphql.operation.query", expectedQueryAttribute));
        expectedOperationAttributes(introspector, expectedTransactionSuffix);
        expectedResolverAttributes(introspector);
        expectedMetrics(introspector);
    }

    private void assertRequestWithArg(String expectedTransactionSuffix, String expectedQueryAttribute) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertOneTxFinishedWithExpectedName(introspector, expectedTransactionSuffix, false);
        assertTrue(attributeValueOnSpan(introspector, expectedTransactionSuffix, "graphql.operation.query", expectedQueryAttribute));
        expectedOperationAttributes(introspector, expectedTransactionSuffix);
        expectedResolverAttributes(introspector);
        expectedMetrics(introspector);
    }

    private void assertErrorOperation(String expectedTransactionSuffix, String spanName, String errorClass, String errorMessage, boolean isParseError) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertOneTxFinishedWithExpectedName(introspector, expectedTransactionSuffix, isParseError);
        assertTrue(attributeValueOnSpan(introspector, spanName, "error.class", errorClass));
        assertTrue(attributeValueOnSpan(introspector, spanName, "error.message", errorMessage));
    }
}
