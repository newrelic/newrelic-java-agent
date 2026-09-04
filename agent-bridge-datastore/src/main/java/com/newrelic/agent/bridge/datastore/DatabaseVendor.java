/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public interface DatabaseVendor {

    String getName();

    String getType();

    boolean isExplainPlanSupported();

    ExplainPlanSqlInfo getExplainPlanSqlInfo(String sqlToExplain) throws SQLException;

    /**
     * If explain plans are supported by the vendor, this tells us if a followup query is required
     * to get the results. Most systems return the explain result as a ResultSet from the initial
     * statement execution, but some (Oracle, for example) require a separate query to another table to
     * fetch the results.
     *
     * @return true if a separate query is required for explain results
     */
    boolean isExplainPlanFollowupQueryRequired();

    /**
     * The SQL for the followup explain query, if required.
     *
     * @param statementId optional String that identifies the key used to fetch the explain results
     *
     * @return the followup explain plan SQL, or null if no followup query is required
     */
    String getFollowupExplainPlanSql(String statementId);

    Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException;

    String getExplainPlanFormat();

    DatastoreVendor getDatastoreVendor();

}
