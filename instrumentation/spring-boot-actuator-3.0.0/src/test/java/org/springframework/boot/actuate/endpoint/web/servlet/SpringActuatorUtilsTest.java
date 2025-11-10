/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.boot.actuate.endpoint.web.servlet;

import com.nr.agent.instrumentation.actuator.SpringActuatorUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpringActuatorUtilsTest {
    @Test
    public void normalizeActuatorUri_withNullValue_returnsNull() {
        assertNull(SpringActuatorUtils.normalizeActuatorUri(null));
    }

    @Test
    public void normalizeActuatorUri_withEmptyString_returnsNull() {
        assertNull(SpringActuatorUtils.normalizeActuatorUri(""));
    }

    @Test
    public void normalizeActuatorUri_withValidUri_returnsModifiedUri() {
        assertEquals("actuator/health", SpringActuatorUtils.normalizeActuatorUri("/actuator/health"));
        assertEquals("actuator/health", SpringActuatorUtils.normalizeActuatorUri("actuator/health"));
    }
}
