/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.bridge.datastore.RecordSql;
import com.nr.agent.instrumentation.jdbc.postgresql_80_jdbc3.PostgresDatabaseVendor;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PostgresDatabaseVendorTest {

    @Test
    public void testparseExplainPlanResultSet_Postgres() throws SQLException, ParseException {
        ResultSet rs = Mockito.mock(ResultSet.class);

        Mockito.when(rs.next()).thenReturn(true);
        String rawExplainPlan = "[{\"Plan\": {\"Node Type\": \"Aggregate\", \"Strategy\": \"Sorted\", \"Startup Cost\": 31.76,\"Total Cost\": 34.19,\"Plan Rows\": 139, \"Plan Width\": 516,\"Plans\": [ {\"Node Type\": \"Sort\",\"Parent Relationship\": \"Outer\", \"Startup Cost\": 31.76,\"Total Cost\": 32.11,\"Plan Rows\": 139, \"Plan Width\": 516,\"Sort Key\": [\"accounts.name\"],\"Plans\": [ {\"Node Type\": \"Hash Join\", \"Parent Relationship\": \"Outer\", \"Join Type\": \"Inner\", \"Startup Cost\": 13.15,\"Total Cost\": 26.81,\"Plan Rows\": 139, \"Plan Width\": 516,\"Hash Cond\": \"(accounts.id = a2.id)\", \"Plans\": [ {\"Node Type\": \"Seq Scan\",\"Parent Relationship\": \"Outer\", \"Relation Name\": \"accounts\",\"Alias\": \"accounts\",\"Startup Cost\": 0.00, \"Total Cost\": 11.75,\"Plan Rows\": 139, \"Plan Width\": 520,\"Filter\": \"((name)::text <> 'dude \\n newline'::text)\" }, {\"Node Type\": \"Hash\",\"Parent Relationship\": \"Inner\", \"Startup Cost\": 11.40,\"Total Cost\": 11.40,\"Plan Rows\": 140, \"Plan Width\": 4,\"Plans\": [ {\"Node Type\": \"Seq Scan\",\"Parent Relationship\": \"Outer\", \"Relation Name\": \"accounts\",\"Alias\": \"a2\",\"Startup Cost\": 0.00, \"Total Cost\": 11.40,\"Plan Rows\": 140, \"Plan Width\": 4 }]}]}]}]}}]";
        Mockito.when(rs.getObject(1)).thenReturn(rawExplainPlan);

        Collection<Collection<Object>> explanation = PostgresDatabaseVendor.INSTANCE.parseExplainPlanResultSet(1, rs,
                RecordSql.obfuscated);
        Assert.assertEquals(1, explanation.size());
        List<Object> parse = (List<Object>) explanation.iterator().next();

        Map<String, Object> map = (Map<String, Object>) parse.get(0);
        Assert.assertEquals(2, verifyObfuscation(map));
    }

    private int verifyObfuscation(Map<String, Object> map) {
        int count = 0;
        Map<String, Object> plan = (Map<String, Object>) map.get("Plan");
        if (plan != null) {
            count += verifyObfuscation(plan);
        }
        JSONArray plans = (JSONArray) map.get("Plans");
        if (plans != null) {
            for (Object o : plans) {
                count += verifyObfuscation((Map<String, Object>) o);
            }
        }
        for (String key : new String[] { "Hash Cond", "Filter" }) {
            Object value = map.get(key);
            if (value != null) {
                Assert.assertEquals("?", value);
                count++;
            }
        }
        return count;
    }

}
