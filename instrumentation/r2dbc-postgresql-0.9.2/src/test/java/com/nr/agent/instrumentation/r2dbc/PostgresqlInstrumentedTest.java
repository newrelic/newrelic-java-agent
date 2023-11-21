/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import static org.junit.Assert.assertEquals;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V9_6;
import static ru.yandex.qatools.embed.postgresql.util.SocketUtil.findFreePort;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "io.r2dbc.postgresql")
public class PostgresqlInstrumentedTest {

    public static final EmbeddedPostgres postgres = new EmbeddedPostgres(V9_6);
    public static Connection connection;

    @Before
    public void setup() throws Exception {
        String databaseName = "Postgres" + System.currentTimeMillis();
        final String url = postgres.start("localhost", findFreePort(), databaseName, "user", "password");
        final String updatedUrl = url.replace("jdbc", "r2dbc").replace("localhost", "user:password@localhost").replace("?user=user&password=password", "");
        ConnectionFactory connectionFactory = ConnectionFactories.get(updatedUrl);
        connection = Mono.from(connectionFactory.create()).block();
        Mono.from(connection.createStatement("CREATE TABLE IF NOT EXISTS USERS(id int primary key, first_name varchar(255), last_name varchar(255), age int);").execute()).block();
        Mono.from(connection.createStatement("TRUNCATE TABLE USERS;").execute()).block();
    }

    @AfterClass
    public static void teardown() {
        Mono.from(connection.close()).block();
        postgres.stop();
    }

    @Test
    public void testBasicRequests() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        DatastoreHelper helper = new DatastoreHelper("Postgres");

        //When
        R2dbcTestUtils.basicRequests(connection);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().stream().findFirst().orElse("");
        helper.assertScopedStatementMetricCount(transactionName, "INSERT", "USERS", 1);
        helper.assertScopedStatementMetricCount(transactionName, "SELECT", "USERS", 3);
        helper.assertScopedStatementMetricCount(transactionName, "UPDATE", "USERS", 1);
        helper.assertScopedStatementMetricCount(transactionName, "DELETE", "USERS", 1);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("INSERT", 1);
        helper.assertUnscopedOperationMetricCount("SELECT", 3);
        helper.assertUnscopedOperationMetricCount("UPDATE", 1);
        helper.assertUnscopedOperationMetricCount("DELETE", 1);
        helper.assertUnscopedStatementMetricCount("INSERT", "USERS", 1);
        helper.assertUnscopedStatementMetricCount("SELECT", "USERS", 3);
        helper.assertUnscopedStatementMetricCount("UPDATE", "USERS", 1);
        helper.assertUnscopedStatementMetricCount("DELETE", "USERS", 1);
    }
}
