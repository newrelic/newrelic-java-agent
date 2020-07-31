/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

class Log4jMarkers {

    /** String for error messages. */
    private static String ERROR_STR = "ERROR";
    /** String for warn messages. */
    private static String WARN_STR = "WARN";
    /** String for info messages. */
    private static String INFO_STR = "INFO";
    /** String for fine messages. */
    private static String FINE_STR = "FINE";
    /** String for finer messages. */
    private static String FINER_STR = "FINER";
    /** String for finest messages. */
    private static String FINEST_STR = "FINEST";
    /** String for debug messages. */
    private static String DEBUG_STR = "DEBUG";
    /** String for trace messages. */
    private static String TRACE_STR = "TRACE";

    /** Used for error messages. */
    public static final Marker ERROR_MARKER = MarkerManager.getMarker(ERROR_STR);
    /** Used for warn messages. */
    public static final Marker WARN_MARKER = MarkerManager.getMarker(WARN_STR);
    /** Used for info messages. */
    public static final Marker INFO_MARKER = MarkerManager.getMarker(INFO_STR);
    /** Used for fine messages. */
    public static final Marker FINE_MARKER = MarkerManager.getMarker(FINE_STR);
    /** Used for finer messages. */
    public static final Marker FINER_MARKER = MarkerManager.getMarker(FINER_STR);
    /** Used for finest messages. */
    public static final Marker FINEST_MARKER = MarkerManager.getMarker(FINEST_STR);
    /** Used for debug messages. */
    public static final Marker DEBUG_MARKER = MarkerManager.getMarker(DEBUG_STR);
    /** Used for trace messages. */
    public static final Marker TRACE_MARKER = MarkerManager.getMarker(TRACE_STR);

}
