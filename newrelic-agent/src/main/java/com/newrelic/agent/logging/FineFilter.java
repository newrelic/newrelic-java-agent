/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.logging.Level;

class FineFilter extends AbstractFilter {

    private static FineFilter instance;

    /**
     * The current java level.
     */
    private volatile Level javaLevel;

    /**
     * The marker to accept if it matches.
     */
    private final Marker markerToMatch = Log4jMarkers.FINE_MARKER;

    /**
     * The marker to fail if it matches.
     */
    private final Marker markerToFail = Log4jMarkers.FINER_MARKER;

    public static FineFilter getFineFilter() {
        if (instance == null) {
            instance = new FineFilter();
        }
        return instance;
    }

    /**
     * Creates this FineFilter.
     */
    private FineFilter() {
        super();
        javaLevel = Level.INFO;
    }

    /**
     * Determines if the input event should be logged.
     *
     * @param event The current event to be evaluated.
     */
    @Override
    public Result filter(LogEvent event) {
        if (!isStarted()) {
            return Result.NEUTRAL;
        }

        if (Level.FINE.equals(javaLevel)) {
            Marker marker = event.getMarker();
            if (marker == null) {
                return Result.NEUTRAL;
            } else if (marker.isInstanceOf(markerToMatch)) {
                return Result.ACCEPT;
            } else if (marker.isInstanceOf(markerToFail)) {
                return Result.DENY;
            }
        }

        return Result.NEUTRAL;
    }

    /**
     * True if the filter is enabled for the level.
     *
     * @param pLevel The current level.
     * @return True if the filter is enabled for the level, else false.
     */
    public boolean isEnabledFor(Level pLevel) {
        return javaLevel.intValue() <= pLevel.intValue();
    }

    /**
     * Sets the level.
     *
     * @param level The new level.
     */
    public void setLevel(Level level) {
        javaLevel = level;
    }

    /**
     * Gets the current level.
     *
     * @return The current level for this filter.
     */
    public Level getLevel() {
        return javaLevel;
    }

    /**
     * Starts this filter.
     */
    @Override
    public void start() {
        if (javaLevel != null) {
            super.start();
        }
    }

}
