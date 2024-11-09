/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import org.flywaydb.core.api.callback.Event;
import org.junit.Test;

import static org.junit.Assert.*;

public class FlywayUtilsTest {
    @Test
    public void isTargetEvent_returnsTrue_forTargetedEvent() {
        assertTrue(FlywayUtils.isTargetEvent(Event.AFTER_EACH_MIGRATE));
        assertTrue(FlywayUtils.isTargetEvent(Event.AFTER_EACH_UNDO));
    }

    @Test
    public void isTargetEvent_returnsFalse_forNonTargetedEvent() {
        assertFalse(FlywayUtils.isTargetEvent(Event.AFTER_CLEAN));
        assertFalse(FlywayUtils.isTargetEvent(Event.AFTER_CLEAN_ERROR));
    }
}
