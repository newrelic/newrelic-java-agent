/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.DatabaseHelper;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PreparedStatementSqlTest {

    @BeforeClass
    public static void initDatabase() throws Exception {
        DatabaseHelper.initDatabase(PreparedStatementSqlTest.class);
    }

    @AfterClass
    public static void shutdownDatabase() {
        try {
            DatabaseHelper.shutdownDatabase(PreparedStatementSqlTest.class);
        } catch (Throwable t) {
            // ignore
        }
    }

    @Test
    public void sql() throws Exception {
        final String originalSql = "select * from test where name = ?";
        TransactionDataList txs = new TransactionDataList();
        ServiceFactory.getTransactionService().addTransactionListener(txs);
        try {
            new StubServlet("dude") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void run(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    Connection connection = DatabaseHelper.getConnection();
                    PreparedStatement statement = connection.prepareStatement(originalSql);
                    statement.setString(1, "test");
                    statement.executeQuery();
                }
            };
        } catch (Exception e) {
            // ignore
        }
        Assert.assertEquals(2, txs.size());
        TransactionData transactionData = txs.get(1);
        String actualSql = null;
        for (Tracer tracer : AgentHelper.getTracers(transactionData.getRootTracer())) {
            if (tracer instanceof SqlTracer) {
                actualSql = ((SqlTracer) tracer).getSql().toString();
            }
        }
        Assert.assertNotNull(actualSql);
        Assert.assertEquals("select * from test where name = 'test'", actualSql);
    }

}
