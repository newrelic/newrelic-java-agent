/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.servlet6;

import java.lang.reflect.Method;
import java.util.logging.Level;

import jakarta.servlet.*;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.NewRelic;

public class ServletHelper {

    private static final ServletHelper INSTANCE = new ServletHelper();

    public static final String APPLICATION_NAME_PARAM = "com.newrelic.agent.APPLICATION_NAME";
    public static final String TRANSACTION_NAME_PARAM = "com.newrelic.agent.TRANSACTION_NAME";

    private static final String CUSTOM_SERVLET_CATEGORY = "CustomServlet";
    private static final String CUSTOM_FILTER_CATEGORY = "CustomFilter";
    private static final String SERVLET_NAME_CATEGORY = "Servlet";
    private static final String FILTER_NAME_CATEGORY = "Filter";
    private static final boolean NO_OVERRIDE = false;
    private Method getContextPathMethod;

    private ServletHelper() {
        try {
            getContextPathMethod = ServletContext.class.getMethod("getContextPath");
        } catch (Exception e) {
            // ignore. This method appeared in servlet 2.5
        }
        AsyncContext asyncContext = null;
    }

    public static void setAppName(ServletConfig servletConfig) {
        if (servletConfig == null || !AgentBridge.getAgent().getTransaction().isAutoAppNamingEnabled()) {
            return;
        }
        if (!setAppNameUsingServletConfigInitParam(servletConfig)) {
            if (!setAppNameUsingServletContextInitParam(servletConfig.getServletContext())) {
                setAppNameUsingServletContext(servletConfig.getServletContext());
            }
        }
    }

    private static void setAppNameUsingServletContext(ServletContext servletContext) {
        if (!AgentBridge.getAgent().getTransaction().isAutoAppNamingEnabled()) {
            return;
        }
        String appName = servletContext.getServletContextName();
        if (appName != null) {
            NewRelic.getAgent().getLogger().log(Level.FINER,
                    "Setting application name using servlet context name parameter: {0}", appName);
            AgentBridge.getAgent().getTransaction().setApplicationName(ApplicationNamePriority.CONTEXT_NAME, appName);
        } else if (INSTANCE.getContextPathMethod != null) {
            try {
                appName = (String) INSTANCE.getContextPathMethod.invoke(servletContext);
                NewRelic.getAgent().getLogger().log(Level.FINER,
                        "Setting application name using servlet context path parameter: {0}", appName);
                AgentBridge.getAgent().getTransaction().setApplicationName(ApplicationNamePriority.CONTEXT_PATH, appName);
            } catch (Exception e) {
                NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Unable to set app name");
            }
        }
    }

    public static void setAppName(FilterConfig filterConfig) {
        if (filterConfig == null || !AgentBridge.getAgent().getTransaction().isAutoAppNamingEnabled()) {
            return;
        }
        if (!setAppNameUsingFilterConfigInitParam(filterConfig)) {
            if (!setAppNameUsingServletContextInitParam(filterConfig.getServletContext())) {
                setAppNameUsingServletContext(filterConfig.getServletContext());
            }
        }
    }

    public static void setTransactionName(ServletConfig servletConfig, Servlet servlet) {
        if (servletConfig == null) {
            return;
        }
        if (setTxNameUsingServletInitParam(servletConfig)) {
            return;
        }
        if (setTxNameUsingServletName(servletConfig, servlet)) {
            return;
        }
        setTxNameUsingServletClassName(servlet.getClass().getSimpleName());
    }

    public static void setTransactionName(FilterConfig filterConfig, Filter filter) {
        if (filterConfig == null) {
            return;
        }
        if (setTxNameUsingFilterInitParam(filterConfig)) {
            return;
        }
        if (setTxNameUsingFilterName(filterConfig)) {
            return;
        }
        setTxNameUsingFilterClassName(filter.getClass().getSimpleName());
    }

    private static boolean setAppNameUsingServletConfigInitParam(ServletConfig servletConfig) {
        if (!AgentBridge.getAgent().getTransaction().isAutoAppNamingEnabled()) {
            return false;
        }
        String appName = servletConfig.getInitParameter(APPLICATION_NAME_PARAM);
        if (appName == null || appName.isEmpty()) {
            return false;
        }
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting application name using servlet init parameter: {0}",
                appName);
        AgentBridge.getAgent().getTransaction().setApplicationName(ApplicationNamePriority.SERVLET_INIT_PARAM, appName);
        return true;
    }

    private static boolean setAppNameUsingFilterConfigInitParam(FilterConfig filterConfig) {
        if (!AgentBridge.getAgent().getTransaction().isAutoAppNamingEnabled()) {
            return false;
        }
        String appName = filterConfig.getInitParameter(APPLICATION_NAME_PARAM);
        if (appName == null || appName.isEmpty()) {
            return false;
        }
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting application name using filter init parameter: {0}",
                appName);
        AgentBridge.getAgent().getTransaction().setApplicationName(ApplicationNamePriority.FILTER_INIT_PARAM, appName);
        return true;
    }

    private static boolean setAppNameUsingServletContextInitParam(ServletContext servletContext) {
        if (!AgentBridge.getAgent().getTransaction().isAutoAppNamingEnabled()) {
            return false;
        }
        String appName = servletContext.getInitParameter(APPLICATION_NAME_PARAM);
        if (appName == null || appName.isEmpty()) {
            return false;
        }
        NewRelic.getAgent().getLogger().log(Level.FINER,
                "Setting application name using servlet context init parameter: {0}", appName);
        AgentBridge.getAgent().getTransaction().setApplicationName(ApplicationNamePriority.CONTEXT_PARAM, appName);
        return true;
    }

    private static boolean setTxNameUsingServletInitParam(ServletConfig servletConfig) {
        String txName = servletConfig.getInitParameter(TRANSACTION_NAME_PARAM);
        if (txName == null || txName.isEmpty()) {
            return false;
        }
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting transaction name using servlet init parameter: {0}",
                txName);
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_INIT_PARAM,
                NO_OVERRIDE, CUSTOM_SERVLET_CATEGORY, txName);
        return true;
    }

    private static boolean setTxNameUsingFilterInitParam(FilterConfig filterConfig) {
        String txName = filterConfig.getInitParameter(TRANSACTION_NAME_PARAM);
        if (txName == null || txName.isEmpty()) {
            return false;
        }
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting transaction name using filter init parameter: {0}",
                txName);
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FILTER_INIT_PARAM,
                NO_OVERRIDE, CUSTOM_FILTER_CATEGORY, txName);
        return true;
    }

    private static boolean setTxNameUsingServletName(ServletConfig servletConfig, Servlet servlet) {
        String txName = servletConfig.getServletName();
        if (txName == null || txName.isEmpty()) {
            return false;
        }
        txName = normalizeServletName(txName, servlet);

        if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.FINEST, "Servlet6/ServletHelper::setTxNameUsingServletName: calling transaction.setTransactionName with " +
                                    "priority: SERVLET_NAME and override false, category: Servlet, txn {0}, ",
                            AgentBridge.getAgent().getTransaction().toString());
        }

        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, NO_OVERRIDE,
                SERVLET_NAME_CATEGORY, txName);
        return true;
    }

    private static String normalizeServletName(String servletName, Servlet servlet) {
        if (servletName.startsWith(servlet.getClass().getName())) {
            // JAVA-59: In Jetty, if the servlet name is not set in web.xml, the container generates a servlet name
            // like this:
            // com.nr.servlet.api.MetricDataServlet-1855515626
            // which creates a metric grouping problem.
            String normalizedServletName = servlet.getClass().getSimpleName();

            NewRelic.getAgent().getLogger().log(Level.FINER, "Normalizing servlet name from \"{0}\" to \"{1}\"",
                    servletName, normalizedServletName);
            return normalizedServletName;
        } else {
            return servletName;
        }
    }

    private static boolean setTxNameUsingFilterName(FilterConfig filterConfig) {
        String txName = filterConfig.getFilterName();
        if (txName == null || txName.isEmpty()) {
            return false;
        }
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting transaction name using filter name: {0}", txName);
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FILTER_NAME, NO_OVERRIDE,
                FILTER_NAME_CATEGORY, txName);
        return true;
    }

    private static void setTxNameUsingServletClassName(String txName) {
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting transaction name using servlet class name: {0}",
                txName);
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.SERVLET_NAME, NO_OVERRIDE,
                SERVLET_NAME_CATEGORY, txName);
    }

    private static void setTxNameUsingFilterClassName(String txName) {
        NewRelic.getAgent().getLogger().log(Level.FINER, "Setting transaction name using filter class name: {0}",
                txName);
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FILTER_NAME, NO_OVERRIDE,
                FILTER_NAME_CATEGORY, txName);
    }

}
