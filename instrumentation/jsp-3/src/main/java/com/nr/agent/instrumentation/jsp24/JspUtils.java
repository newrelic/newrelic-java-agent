/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jsp24;

import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;

public class JspUtils {
    public static final String JSP_CATEGORY = "JSP";
    public static final String ORG_APACHE_JSP = "org.apache.jsp.";
    public static final Pattern JSP_PATTERN = Pattern.compile("_jsp$");
    public static final Pattern WEB_INF_PATTERN = Pattern.compile("WEB_002dINF");

    public static void setTransactionName(Class<?> jspClass, TracedMethod timedMethod) {
        String name = jspClass.getName();
        try {
            if (name.startsWith(ORG_APACHE_JSP)) {
                name = name.substring(ORG_APACHE_JSP.length()).replace('.', '/');
                name = WEB_INF_PATTERN.matcher(name).replaceFirst("WEB-INF");
            } else {
                int index = name.lastIndexOf('.');
                if (index > 0) {
                    name = name.substring(index + 1);
                }
            }
            name = JSP_PATTERN.matcher(name).replaceAll(".jsp");
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.JSP, false,
                    JSP_CATEGORY, name);

            timedMethod.setMetricName("Jsp", name);
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "An error occurred formatting a jsp name : {0}", e);
        }
    }

}
