/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.jdbc.oracle;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.ExplainPlanSqlInfo;
import com.newrelic.agent.bridge.datastore.RecordSql;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OracleDatabaseVendorTest {
    @Test
    public void getDatastoreVendor_returnsOracle() {
        assertEquals(DatastoreVendor.Oracle, OracleDatabaseVendor.INSTANCE.getDatastoreVendor());
    }

    @Test
    public void getExplainPlanSqlInfo_returnsCorrectExplainPlanSqlInfoInstance() throws SQLException {
        ExplainPlanSqlInfo explainPlanSqlInfo = OracleDatabaseVendor.INSTANCE.getExplainPlanSqlInfo("select * from foo where id = 1");
        assertEquals("EXPLAIN PLAN SET STATEMENT_ID = '0' FOR select * from foo where id = 1", explainPlanSqlInfo.getSql());
        assertEquals("0", explainPlanSqlInfo.getStatementId());
    }

    @Test
    public void isExplainPlanFollowupQueryRequired_returnsTrue() {
        assertTrue(OracleDatabaseVendor.INSTANCE.isExplainPlanFollowupQueryRequired());
    }

    @Test
    public void getFollowupExplainPlanSql_returndCorrectSql() {
        assertEquals("SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY('PLAN_TABLE', '0', 'TYPICAL'))",
                OracleDatabaseVendor.INSTANCE.getFollowupExplainPlanSql("0"));
    }

    @Test
    public void parseExplainPlanResultSet_joinsLinesIntoSingleTextBlock() throws SQLException {
        // Example DBMS_XPLAN.DISPLAY output for:
        // SELECT e.employee_id, e.last_name FROM employees e WHERE e.salary > 5000
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, true, true, false);
        when(rs.getString(1)).thenReturn(
                "Plan hash value: 1234567890",
                "",
                "-----------------------------------------------------------",
                "| Id  | Operation         | Name             | Rows  | Cost |");

        Collection<Collection<Object>> explainPlan = OracleDatabaseVendor.INSTANCE.parseExplainPlanResultSet(1, rs, RecordSql.raw);

        assertEquals(1, explainPlan.size());
        Collection<Object> row = explainPlan.iterator().next();
        assertEquals(1, row.size());
        assertEquals("Plan hash value: 1234567890\n\n-----------------------------------------------------------\n"
                + "| Id  | Operation         | Name             | Rows  | Cost |", row.iterator().next());
    }
}
