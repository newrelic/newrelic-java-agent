package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Test;

public class R2dbcOperationTest {

    @Test
    public void extractFrom_allOperations() {
        assertOperationAndTable("select * from schema.\"myTable\"", "SELECT", "schema.myTable");
        assertOperationAndTable("insert into t values ('value')", "INSERT", "t");
        assertOperationAndTable("update myTable set col='value'", "UPDATE", "myTable");
        assertOperationAndTable("delete * from myTable", "DELETE", "myTable");
        assertOperationAndTable("with myTable as (select * from dual)", "WITH", "myTable");
        assertOperationAndTable("call myProcedure()", "CALL", "myProcedure");
        assertOperationAndTable("exec myProcedure()", "EXEC", "myProcedure");
    }

    private void assertOperationAndTable(String sql, String operation, String table) {
        OperationAndTableName result = R2dbcOperation.extractFrom(sql);
        Assert.assertEquals(operation, result.getOperation());
        Assert.assertEquals(table, result.getTableName());
    }
}
