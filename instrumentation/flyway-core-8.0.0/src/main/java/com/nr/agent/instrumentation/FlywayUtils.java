/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.api.agent.NewRelic;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Event;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class FlywayUtils {
    private static final String EVENT_NAME = "FlywayMigration";
    public static final String ATTR_SUCCESS = "migrationSuccess";
    public static final String ATTR_PHYSICAL_LOCATION = "migrationFilePhysicalLocation";
    public static final String ATTR_CHECKSUM = "migrationChecksum";
    public static final String ATTR_VERSION = "migrationVersion";
    public static final String ATTR_SCRIPT = "migrationScriptName";
    public static final String FLYWAY_EVENT = "migrationEvent";

    // Map of Flyway events we're interested in with a boolean value that denotes a
    // successful or failed migration step
    private static final EnumMap<Event, Boolean> TARGET_EVENTS = new EnumMap<>(Event.class);
    static {
        TARGET_EVENTS.put(Event.AFTER_EACH_MIGRATE, true);
        TARGET_EVENTS.put(Event.AFTER_EACH_MIGRATE_ERROR, false);
        TARGET_EVENTS.put(Event.AFTER_EACH_UNDO, true);
        TARGET_EVENTS.put(Event.AFTER_EACH_UNDO_ERROR, false);
    }

    public static boolean isTargetEvent(Event event) {
        return TARGET_EVENTS.containsKey(event);
    }

    public static void submitFlywayEvent(Event event, MigrationInfo migrationInfo) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTR_SUCCESS, isSuccessfulEvent(event).toString());
        attributes.put(ATTR_PHYSICAL_LOCATION, migrationInfo.getPhysicalLocation());
        attributes.put(ATTR_CHECKSUM, migrationInfo.getChecksum().toString());
        attributes.put(ATTR_VERSION, migrationInfo.getVersion().getVersion());
        attributes.put(ATTR_SCRIPT, migrationInfo.getScript());
        attributes.put(FLYWAY_EVENT, event.toString());
        NewRelic.getAgent().getInsights().recordCustomEvent(EVENT_NAME, attributes);
    }

    private static Boolean isSuccessfulEvent(Event event) {
        return TARGET_EVENTS.get(event);
    }

}
