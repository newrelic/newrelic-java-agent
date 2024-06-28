/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jsp4;

import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;

public class JspUtils {
    public static final String JSP_CATEGORY = "JSP";
    public static final String ORG_APACHE_JSP = "org.apache.jsp.";
    public static final Pattern JSP_PATTERN = Pattern.compile("_jsp$");
    public static final Pattern WEB_INF_PATTERN = Pattern.compile("WEB_002dINF");
    private static final String AUTO_INSTRUMENT_ENABLED_CONFIG = "browser_monitoring.auto_instrument";
    private static final String TAG_LIB_ENABLED_CONFIG = "browser_monitoring.tag_lib_instrument";
    private static final String TAG_LIB_HEAD_REGEX = "browser_monitoring.tag_lib_head_pattern";
    private static final String TAG_LIB_HEAD_REGEX_DEFAULT = "<head>";

    public static final Pattern START_HEAD_REGEX = generateStartHeadElementRegExPattern();

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

    public static boolean isTagLibInstrumentationEnabled() {
        return NewRelic.getAgent().getConfig().getValue(AUTO_INSTRUMENT_ENABLED_CONFIG, Boolean.FALSE) &&
                NewRelic.getAgent().getConfig().getValue(TAG_LIB_ENABLED_CONFIG, Boolean.FALSE);
    }

    private static Pattern generateStartHeadElementRegExPattern() {
        String regexString = NewRelic.getAgent().getConfig().getValue(TAG_LIB_HEAD_REGEX, TAG_LIB_HEAD_REGEX_DEFAULT);
        Pattern pattern;

        try {
            pattern = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
        } catch (PatternSyntaxException e) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, "Invalid pattern defined for tag lib start head regex: {0}  Defaulting to: {1}",
                    regexString, TAG_LIB_HEAD_REGEX_DEFAULT);
            pattern = Pattern.compile(TAG_LIB_HEAD_REGEX_DEFAULT, Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
        }

        if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Tag lib start head regex to be used for RUM script injection: {0}", pattern.pattern());
        }

        return pattern;
    }
}
