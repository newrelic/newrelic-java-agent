/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.r2dbc;

import com.newrelic.api.agent.Trace;
import io.r2dbc.spi.Connection;
import reactor.core.publisher.Mono;

public class R2dbcTestUtils {
    @Trace(dispatcher = true)
    public static void basicRequests(Connection connection) {
        Mono.from(connection.createStatement("INSERT INTO USERS(id, first_name, last_name, age) VALUES(1, 'Max', 'Power', 30)").execute()).block();
        Mono.from(connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute()).block();
        Mono.from(connection.createStatement("UPDATE USERS SET age = 36 WHERE last_name = 'Power'").execute()).block();
        Mono.from(connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute()).block();
        Mono.from(connection.createStatement("DELETE FROM USERS WHERE last_name = 'Power'").execute()).block();
        Mono.from(connection.createStatement("SELECT * FROM USERS").execute()).block();
    }
}
