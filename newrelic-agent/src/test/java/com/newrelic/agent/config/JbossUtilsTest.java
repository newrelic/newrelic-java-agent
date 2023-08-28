/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.instrument.Instrumentation;

public class JbossUtilsTest {
    @Test
    public void checkAndApplyJbossAdjustments_when_sysPropNotSet() {
        String startVal = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
        try {
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "java.util.logging,javax.management,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
        }
    }

    @Test
    public void checkAndApplyJbossAdjustments_when_sysPropSet() {
        String startVal = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
        try {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, "org.junit");
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "org.junit,java.util.logging,javax.management,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
        }
    }
}
