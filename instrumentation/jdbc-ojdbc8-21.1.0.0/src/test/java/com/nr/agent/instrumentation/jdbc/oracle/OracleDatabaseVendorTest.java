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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

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
        assertEquals("SELECT * FROM PLAN_TABLE WHERE STATEMENT_ID = '0'", OracleDatabaseVendor.INSTANCE.getFollowupExplainPlanSql("0"));
    }

    @Test
    public void parseExplainPlanResultSet_withRawSql_returnsParsedPlan() throws SQLException {
        // Example SQL and resulting PLAN_TABLE contents used for the test
        /*
            EXPLAIN PLAN SET STATEMENT_ID = 'demo_query' FOR
            SELECT e.employee_id, e.last_name, d.department_name
            FROM employees e
            JOIN departments d ON e.department_id = d.department_id
            WHERE e.salary > 5000;
         */
        /*
            ID  OPERATION         OPTIONS          OBJECT_NAME    COST  CARDINALITY  BYTES  ACCESS_PREDICATES                         FILTER_PREDICATES
            ---------------------------------------------------------------------------------------------------------------------------------------------
            0   SELECT STATEMENT  null             null           6     12           480    null                                      null
            1   HASH JOIN         null             null           6     12           480    "E"."DEPARTMENT_ID"="D"."DEPARTMENT_ID"   null
            2   TABLE ACCESS      FULL             DEPARTMENTS    2     27           432    null                                      null
            3   TABLE ACCESS      BY INDEX ROWID   EMPLOYEES      4     12           288    null                                      null
            4   INDEX             RANGE SCAN       EMP_SALARY_IX  1     15           null   "E"."SALARY">5000                         null
         */
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, true, true, true, false);

        when(rs.getObject("ID")).thenReturn(0, 1, 2, 3, 4);
        when(rs.getObject("OPERATION")).thenReturn("SELECT STATEMENT", "HASH JOIN", "TABLE ACCESS", "TABLE ACCESS", "INDEX");
        when(rs.getObject("OPTIONS")).thenReturn(null, null, "FULL", "BY INDEX ROWID", "RANGE SCAN");
        when(rs.getObject("OBJECT_NAME")).thenReturn(null, null, "DEPARTMENTS", "EMPLOYEES", "EMP_SALARY_IX");
        when(rs.getObject("COST")).thenReturn(6, 6, 2, 4, 1);
        when(rs.getObject("CARDINALITY")).thenReturn(12, 12, 27, 12, 15);
        when(rs.getObject("BYTES")).thenReturn(480, 480, 432, 288, null);
        when(rs.getObject("ACCESS_PREDICATES")).thenReturn(null,
                "\"E\".\"DEPARTMENT_ID\"=\"D\".\"DEPARTMENT_ID\"", null, null, "\"E\".\"SALARY\">5000");
        when(rs.getObject("FILTER_PREDICATES")).thenReturn(null);

        Collection<Collection<Object>> explainPlan = OracleDatabaseVendor.INSTANCE.parseExplainPlanResultSet(9, rs, RecordSql.raw);

        Iterator<Collection<Object>> rows = explainPlan.iterator();
        assertEquals(Arrays.asList("0", "SELECT STATEMENT", "", "", "6", "12", "480", "", ""), rows.next());
        assertEquals(Arrays.asList("1", "HASH JOIN", "", "", "6", "12", "480",
                "\"E\".\"DEPARTMENT_ID\"=\"D\".\"DEPARTMENT_ID\"", ""), rows.next());
        assertEquals(Arrays.asList("2", "TABLE ACCESS", "FULL", "DEPARTMENTS", "2", "27", "432", "", ""), rows.next());
        assertEquals(Arrays.asList("3", "TABLE ACCESS", "BY INDEX ROWID", "EMPLOYEES", "4", "12", "288", "", ""), rows.next());
        assertEquals(Arrays.asList("4", "INDEX", "RANGE SCAN", "EMP_SALARY_IX", "1", "15", "",
                "\"E\".\"SALARY\">5000", ""), rows.next());
        assertFalse(rows.hasNext());
    }

    @Test
    public void parseExplainPlanResultSet_withObfuscatedRecordSql_removesPredicates() throws SQLException {
        // Same plan data as test above
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, true, true, true, false);

        when(rs.getObject("ID")).thenReturn(0, 1, 2, 3, 4);
        when(rs.getObject("OPERATION")).thenReturn("SELECT STATEMENT", "HASH JOIN", "TABLE ACCESS", "TABLE ACCESS", "INDEX");
        when(rs.getObject("OPTIONS")).thenReturn(null, null, "FULL", "BY INDEX ROWID", "RANGE SCAN");
        when(rs.getObject("OBJECT_NAME")).thenReturn(null, null, "DEPARTMENTS", "EMPLOYEES", "EMP_SALARY_IX");
        when(rs.getObject("COST")).thenReturn(6, 6, 2, 4, 1);
        when(rs.getObject("CARDINALITY")).thenReturn(12, 12, 27, 12, 15);
        when(rs.getObject("BYTES")).thenReturn(480, 480, 432, 288, null);
        when(rs.getObject("ACCESS_PREDICATES")).thenReturn(null,
                "\"E\".\"DEPARTMENT_ID\"=\"D\".\"DEPARTMENT_ID\"", null, null, "\"E\".\"SALARY\">5000");
        when(rs.getObject("FILTER_PREDICATES")).thenReturn(null);

        Collection<Collection<Object>> explainPlan = OracleDatabaseVendor.INSTANCE.parseExplainPlanResultSet(9, rs, RecordSql.obfuscated);

        // Every row's ACCESS_PREDICATES/FILTER_PREDICATES column is replaced with "?", similar to postgres
        Iterator<Collection<Object>> rows = explainPlan.iterator();
        assertEquals(Arrays.asList("0", "SELECT STATEMENT", "", "", "6", "12", "480", "?", "?"), rows.next());
        assertEquals(Arrays.asList("1", "HASH JOIN", "", "", "6", "12", "480", "?", "?"), rows.next());
        assertEquals(Arrays.asList("2", "TABLE ACCESS", "FULL", "DEPARTMENTS", "2", "27", "432", "?", "?"), rows.next());
        assertEquals(Arrays.asList("3", "TABLE ACCESS", "BY INDEX ROWID", "EMPLOYEES", "4", "12", "288", "?", "?"), rows.next());
        assertEquals(Arrays.asList("4", "INDEX", "RANGE SCAN", "EMP_SALARY_IX", "1", "15", "", "?", "?"), rows.next());
        assertFalse(rows.hasNext());
    }
}
