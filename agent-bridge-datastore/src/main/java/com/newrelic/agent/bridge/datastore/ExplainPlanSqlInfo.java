/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.datastore;

/**
 * Class to hold information about what's required to run an explain plan.
 */
public class ExplainPlanSqlInfo {
    private final String sql;

    private final String statementId;

    public ExplainPlanSqlInfo(String sql, String statementId) {
        this.sql = sql;
        this.statementId = statementId;
    }

    /**
     * The SQL required to execute the explain plan
     *
     * @return the explain plan SQL
     */
    public String getSql() {
        return sql;
    }

    /**
     * Optional String that identifies the key used to store and fetch the explain results, if required
     * by the underlying DBMS
     *
     * @return the statement id String or null if it's not required
     */
    public String getStatementId() {
        return statementId;
    }
}
