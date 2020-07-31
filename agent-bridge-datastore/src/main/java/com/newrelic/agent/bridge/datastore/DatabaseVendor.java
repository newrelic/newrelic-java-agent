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

    String getExplainPlanSql(String sql) throws SQLException;

    Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException;

    String getExplainPlanFormat();

    DatastoreVendor getDatastoreVendor();

}
