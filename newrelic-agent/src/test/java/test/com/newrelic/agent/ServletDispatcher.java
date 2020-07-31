/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.com.newrelic.agent;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;

import com.newrelic.agent.FakeRequest;
import com.newrelic.agent.FakeResponse;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.api.agent.Trace;

public class ServletDispatcher {

    private static class ServletExceptionWithStatus extends ServletException {
        private static final long serialVersionUID = 1L;

        private final int status;

        public ServletExceptionWithStatus(int status) {
            this.status = status;
        }
    }

    public static Tracer getServletDispatcher(FakeRequest request, final FakeResponse response) {
        ClassMethodSignature sig = new ClassMethodSignature("Dispatcher", "dispatch", "()V");
        return new OtherRootTracer(Transaction.getTransaction(), sig, request, new ClassMethodMetricNameFormat(sig,
                request));
    }

    public static void registerTracerFactory() {
        ServiceFactory.getTracerService().registerTracerFactory("testServletDispatcher", new AbstractTracerFactory() {

            @Override
            public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
                return getServletDispatcher((FakeRequest) args[1], (FakeResponse) args[2]);
            }

        });
    }

    private static final String UNKNOWN_SERVLET = "Unknown servlet";

    @Trace(tracerFactoryName = "testServletDispatcher")
    public static void dispatch(Servlet servlet, MockHttpServletRequest request, MockHttpServletResponse response)
            throws ServletException, IOException {
        if (servlet == null) {
            servlet = new HttpServlet() {

                @Override
                protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
                        IOException {
                    resp.setStatus(404);

                    throw new ServletException(UNKNOWN_SERVLET);
                }

            };
        }

        try {
            servlet.service(request, response);
        } catch (ServletException ex) {
            if (!UNKNOWN_SERVLET.equals(ex.getMessage())) {
                response.setStatus(500);
            }
            throw ex;
        }

        // return getTransaction();
    }
}
