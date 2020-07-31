/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

public class DummyJsp extends HttpServlet implements HttpJspPage {
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    public void _jspService(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
    }

    public void jspDestroy() {
    }

    public void jspInit() {
    }

}
