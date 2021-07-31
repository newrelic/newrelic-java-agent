package com.nr.instrumentation.graphql;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Optional;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"graphql", "com.nr.instrumentation"}, configName = "distributed_tracing.yml")
public class GraphQL_InstrumentationTest {
    private static final long DEFAULT_TIMEOUT_IN_MILLIS = 10_000;

    private static GraphQL graphQL;

    @BeforeClass
    public static void initialize() {
        String schema = "type Query{hello(arg: String): String}";

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

    @Test
    public void test() {
        //given
        String query = "{hello}";
        //when
        trace(createRunnable(query));
        //then
        assertOperation("QUERY/<anonymous>/hello");
    }

    @Test
    public void testWithArg() {
        //given
        String query = "{hello (arg: \"foo\")}";
        //when
        trace(createRunnable(query));
        //then
        assertOperation("QUERY/<anonymous>/hello");
    }

    @Test
    public void parsingError() {
        //given
        String query = "cause a parse error";
        //when
        trace(createRunnable(query));
        //then
        assertParseErrorOperation("post/*");
    }

    @Ignore
    @Test
    public void validationError() {
        //given
        String query = "{noSuchField}";
        //when
        trace(createRunnable(query));
        //then
        //fixme this test doesn't pass, just for triggering code path
        assertOperation("QUERY/<anonymous>/hello");
    }

    @Trace(dispatcher = true)
    private void trace(Runnable runnable) {
        runnable.run();
    }

    @Trace(dispatcher = true)
    private void trace(Runnable[] actions) {
        Arrays.stream(actions).forEach(Runnable::run);
    }

    private void assertOperation(String expectedTransactionSuffix) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_IN_MILLIS));

        String txName = introspector.getTransactionNames().iterator().next();
        assertEquals("Transaction name is incorrect",
                "OtherTransaction/GraphQL/" + expectedTransactionSuffix, txName);
    }

    private void assertParseErrorOperation(String expectedTransactionSuffix) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(DEFAULT_TIMEOUT_IN_MILLIS));

        String txName = introspector.getTransactionNames().iterator().next();
        assertEquals("Transaction name is incorrect",
                "OtherTransaction/" + expectedTransactionSuffix, txName);

        assertTrue(
                introspector.getSpanEvents().stream().anyMatch(spanEvent -> {
           Optional<String> value = Optional.ofNullable((String) spanEvent.getAgentAttributes().get("error.class"));
            return value.map(s -> s.contains("InvalidSyntaxException")).orElse(false);
        })
        );
    }

    private Runnable createRunnable(final String query){
        return () -> graphQL.execute(query);
    }
}
