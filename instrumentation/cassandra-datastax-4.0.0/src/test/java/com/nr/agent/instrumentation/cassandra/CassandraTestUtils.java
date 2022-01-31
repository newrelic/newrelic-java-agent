/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.newrelic.api.agent.Trace;

public class CassandraTestUtils {
    @Trace(dispatcher = true)
    public static void syncBasicRequests(CqlSession session) {
        session.execute("INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')");
        session.execute("SELECT * FROM users WHERE lastname='Jones'");
        session.execute("UPDATE users SET age = 36 WHERE lastname = 'Jones'");
        session.execute("SELECT * FROM users WHERE lastname='Jones'");
        session.execute("DELETE FROM users WHERE lastname = 'Jones'");
        session.execute("SELECT * FROM users");
    }

    @Trace(dispatcher = true)
    public static void asyncBasicRequests(CqlSession session) {
        session.executeAsync("SELECT * FROM users WHERE lastname='Jones'");
        session.executeAsync("SELECT * FROM users WHERE lastname='Jones'");
    }

    @Trace(dispatcher = true)
    public static void statementRequests(CqlSession session) {
        session.execute(SimpleStatement.builder("INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')").build());
        session.execute(SimpleStatement.builder("SELECT * FROM users WHERE lastname='Jones'").build());
        session.execute(SimpleStatement.builder("UPDATE users SET age = 36 WHERE lastname = 'Jones'").build());
        session.execute(SimpleStatement.builder("SELECT * FROM users WHERE lastname='Jones'").build());
        session.execute(SimpleStatement.builder("DELETE FROM users WHERE lastname = 'Jones'").build());
        session.execute(SimpleStatement.builder("SELECT * FROM users").build());
    }

    @Trace(dispatcher = true)
    public static void batchStatementRequests(CqlSession session) {
        session.execute(BatchStatement.builder(BatchType.LOGGED)
                .addStatement(SimpleStatement.builder("INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')").build())
                .addStatement(SimpleStatement.builder("INSERT INTO users2 (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')").build())
                .addStatement(SimpleStatement.builder("DELETE FROM users WHERE lastname = 'Jones'").build())
                .addStatement(SimpleStatement.builder("DELETE FROM users2 WHERE lastname = 'Jones'").build())
                .build());
    }

    @Trace(dispatcher = true)
    public static void syncRequestError(CqlSession session) {
        try {
            session.execute("SELECT * FORM users WHERE lastname='Jones'");
        } catch (Exception ignored) {
        }
    }

    @Trace(dispatcher = true)
    public static void asyncRequestError(CqlSession session) {
        try {
            session.executeAsync("SELECT * FORM users WHERE lastname='Jones'");
        } catch (Exception ignored) {
        }
    }
}
