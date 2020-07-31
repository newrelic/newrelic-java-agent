/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.jsp.jsp.jsp2.el;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

/**
 * A fake JSP.
 */
public class functions_jsp extends HttpServlet implements HttpJspPage {

    private static final long serialVersionUID = -1394396801371311812L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        _jspService(req, resp);
    }

    @Override
    public void jspInit() {
    }

    @Override
    public void jspDestroy() {
    }

    @Override
    public void _jspService(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse)
            throws ServletException, IOException {

    }
}
