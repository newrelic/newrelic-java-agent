/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;

import java.util.HashMap;
import java.util.Map;

enum Log4jLevel {

    /** The off level. */
    OFF("off", Level.OFF, java.util.logging.Level.OFF, null),
    /** The level all. */
    ALL("all", Level.ALL, java.util.logging.Level.ALL, null),
    /** The fatal level. */
    FATAL("fatal", Level.ERROR, java.util.logging.Level.SEVERE, Log4jMarkers.ERROR_MARKER),
    /** The level severe. */
    SEVERE("severe", Level.ERROR, java.util.logging.Level.SEVERE, Log4jMarkers.ERROR_MARKER),
    /** The error level. */
    ERROR("error", Level.ERROR, java.util.logging.Level.SEVERE, Log4jMarkers.ERROR_MARKER),
    /** The warn level. */
    WARN("warn", Level.WARN, java.util.logging.Level.WARNING, Log4jMarkers.WARN_MARKER),
    /** The warning level. */
    WARNING("warning", Level.WARN, java.util.logging.Level.WARNING, Log4jMarkers.WARN_MARKER),
    /** The info level. */
    INFO("info", Level.INFO, java.util.logging.Level.INFO, Log4jMarkers.INFO_MARKER),
    /** The config level. */
    CONFIG("config", Level.INFO, java.util.logging.Level.CONFIG, Log4jMarkers.INFO_MARKER),
    /** The fine level. */
    FINE("fine", Level.DEBUG, java.util.logging.Level.FINE, Log4jMarkers.FINE_MARKER),
    /** The finer level. */
    FINER("finer", Level.DEBUG, java.util.logging.Level.FINER, Log4jMarkers.FINER_MARKER),
    /** The finest level. */
    FINEST("finest", Level.TRACE, java.util.logging.Level.FINEST, Log4jMarkers.FINEST_MARKER),
    /** The debug level. */
    DEBUG("debug", Level.DEBUG, java.util.logging.Level.FINE, Log4jMarkers.DEBUG_MARKER),
    /** The trace level. */
    TRACE("trace", Level.TRACE, java.util.logging.Level.FINEST, Log4jMarkers.TRACE_MARKER);

    /**
     * Name of the level.
     */
    private final String name;
    /** The log4j level: ERROR, WARN, INFO, DEBUG, or TRACE. */
    private final Level log4jLevel;
    /** The java util level. */
    private final java.util.logging.Level javaLevel;
    /** The marker for the level. */
    private final Marker marker;

    /**
     * Key is the string value of the level. Value is the associated log4j level.
     */
    private static final Map<String, Log4jLevel> CONVERSION = new HashMap<>();
    /**
     * Key is the java.util logging level. Value is the associated log4j level.
     */
    private static final Map<java.util.logging.Level, Log4jLevel> JAVA_TO_LOG4J = new HashMap<>();

    static {
        Log4jLevel[] levels = Log4jLevel.values();
        for (Log4jLevel level : levels) {
            CONVERSION.put(level.name, level);
        }

        JAVA_TO_LOG4J.put(java.util.logging.Level.ALL, ALL);
        JAVA_TO_LOG4J.put(java.util.logging.Level.FINER, FINER);
        JAVA_TO_LOG4J.put(java.util.logging.Level.FINEST, FINEST);
        JAVA_TO_LOG4J.put(java.util.logging.Level.FINE, FINE);
        JAVA_TO_LOG4J.put(java.util.logging.Level.WARNING, WARNING);
        JAVA_TO_LOG4J.put(java.util.logging.Level.SEVERE, SEVERE);
        JAVA_TO_LOG4J.put(java.util.logging.Level.CONFIG, CONFIG);
        JAVA_TO_LOG4J.put(java.util.logging.Level.INFO, INFO);
        JAVA_TO_LOG4J.put(java.util.logging.Level.OFF, OFF);
    }

    /**
     *
     * Creates this Log4jLevel.
     *
     * @param pName Name of the level.
     * @param pLog4jLevel Associated log4j level.
     * @param pJavaLevel Associated java.util level.
     * @param pMarker Associated marker.
     */
    Log4jLevel(String pName, Level pLog4jLevel, java.util.logging.Level pJavaLevel, Marker pMarker) {
        name = pName;
        log4jLevel = pLog4jLevel;
        javaLevel = pJavaLevel;
        marker = pMarker;
    }

    /**
     * Gets the field marker.
     *
     * @return the marker
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * Gets the field log4jLevel.
     *
     * @return the log4jLevel
     */
    public Level getLog4jLevel() {
        return log4jLevel;
    }

    /**
     * Gets the field javaLevel.
     *
     * @return the javaLevel
     */
    public java.util.logging.Level getJavaLevel() {
        return javaLevel;
    }

    /**
     * Returns the log4j level associated with the input string.
     *
     * @param pName The name of the level.
     * @param pDefault The level.
     * @return The associated log4j level.
     */
    public static Log4jLevel getLevel(String pName, Log4jLevel pDefault) {
        Log4jLevel level = CONVERSION.get(pName);
        return ((level == null) ? pDefault : level);
    }

    /**
     * Returns the log4j level associated with the input level.
     *
     * @param pName The name of the level.
     * @return The associated logj4 level.
     */
    public static Log4jLevel getLevel(java.util.logging.Level pName) {
        return JAVA_TO_LOG4J.get(pName);
    }

}
