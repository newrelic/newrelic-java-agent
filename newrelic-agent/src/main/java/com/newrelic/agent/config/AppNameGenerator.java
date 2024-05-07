/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.logging.Level;

public class AppNameGenerator {
    // This property will return the runnable class/jar file and associated app args passed to the java command
    // to start the application. This is not guaranteed to be present in all Java distros.
    private static final String STARTUP_CMD_PROPERTY = "sun.java.command";
    private static final String JAR_SUFFIX = ".jar";
    private static final String SPACE_DELIM = " ";

    /**
     * Attempt to generate an app name using various techniques (in order):
     * <ul>
     *   <li>Using the "sun.java.command" system property</li>
     *   <li>Use the last element of the stacktrace</li>
     * </ul>
     *
     * @return a generated app name, if it was able to be successfully generated, null otherwise
     */
    public static String generateAppName() {
        Agent.LOG.log(Level.INFO, "Attempting to generate an app_name property");
        String startupCmdAppName = fromStartupCmdProperty();
        if (startupCmdAppName != null) {
            Agent.LOG.log(Level.INFO, "app_name property generated from \"" + STARTUP_CMD_PROPERTY + "\" system property: {0}", startupCmdAppName);
            return startupCmdAppName;
        }

        String stackTraceAppName = fromStackTrace();
        if (stackTraceAppName != null) {
            Agent.LOG.log(Level.INFO, "app_name property generated from stack trace: {0}", stackTraceAppName);
            return stackTraceAppName;
        }

        return null;
    }

    /**
     * Query the "sun.java.command" system property and attempt the derive an app name from the
     * runnable class or jar file.
     *
     * @return the app name derived from the system property or null if the system property
     * doesn't exist
     */
    private static String fromStartupCmdProperty() {
        String cmdlineProperty = System.getProperty(STARTUP_CMD_PROPERTY);
        Agent.LOG.log(Level.FINEST, "Value from \"" + STARTUP_CMD_PROPERTY + "\" system property: {0}", cmdlineProperty);

        if (StringUtils.isNotBlank(cmdlineProperty)) {
            // Interested in the 1st token, stripping the ".jar" suffix if present
            return new StringTokenizer(cmdlineProperty, SPACE_DELIM).nextToken().replace(JAR_SUFFIX, "");
        }

        return null;
    }

    /**
     * Derive an app name from the class of the final frame of the current thread's stack trace.
     *
     * @return the app name derived from the stack trace or null if the stack trace isn't
     * available
     */
    private static String fromStackTrace() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (trace.length > 0) {
            Agent.LOG.log(Level.FINEST, "Extracting app_name from final frame of stack trace: {0}", trace[trace.length - 1].toString());
            return trace[trace.length - 1].getClassName();
        }

        return null;
    }
}
