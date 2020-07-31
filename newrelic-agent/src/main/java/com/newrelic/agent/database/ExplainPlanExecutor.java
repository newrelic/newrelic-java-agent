/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;

public interface ExplainPlanExecutor {

    void runExplainPlan(DatabaseService databaseService, Connection connection, DatabaseVendor vendor)
            throws SQLException;
}