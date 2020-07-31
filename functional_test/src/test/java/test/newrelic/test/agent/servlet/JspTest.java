/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.servlet;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.jsp.jsp.jsp2.el.functions_jsp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.Transaction;

public class JspTest {
    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    @Test
    public void test() throws ServletException, IOException {
        functions_jsp servlet = new functions_jsp();
        String path = "/my/word";
        Transaction tx = AgentHelper.invokeServlet(servlet, "", "Test", path);

        Assert.assertEquals("WebTransaction/JSP/jsp/jsp2/el/functions.jsp", tx.getPriorityTransactionName().getName());

    }
}
