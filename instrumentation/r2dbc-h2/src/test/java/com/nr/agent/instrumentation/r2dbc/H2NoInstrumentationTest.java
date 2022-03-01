package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "none")
public class H2NoInstrumentationTest {

    public Connection connection;

    @Before
    public void setup() {
        ConnectionFactory connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///test");
        connection = Mono.from(connectionFactory.create()).block();
        Mono.from(connection.createStatement("CREATE TABLE IF NOT EXISTS USERS(id int primary key, first_name varchar(255), last_name varchar(255), age int)").execute()).block();
        Mono.from(connection.createStatement("TRUNCATE TABLE USERS").execute()).block();
    }

    @Test
    public void testSelect() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        DatastoreHelper helper = new DatastoreHelper(DatastoreVendor.H2.toString());

        //When
        R2dbcTestUtils.basicRequests(connection);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().stream().findFirst().orElse("");
        helper.assertScopedStatementMetricCount(transactionName, "INSERT", "USERS", 0);
        helper.assertScopedStatementMetricCount(transactionName, "SELECT", "USERS", 0);
        helper.assertScopedStatementMetricCount(transactionName, "UPDATE", "USERS", 0);
        helper.assertScopedStatementMetricCount(transactionName, "DELETE", "USERS", 0);
    }
}