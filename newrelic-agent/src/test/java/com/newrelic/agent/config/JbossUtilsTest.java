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
    public void applyJbossAdjustments() {
        String startVal = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
        try {
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "javax.management,java.util.logging,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
        }
    }

    @Test
    public void applyJbossAdjustments_when_jsr77Fix_is_true() {
        String startVal = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
        String startJsr77FixVal = System.getProperty(JbossUtils.NR_JBOSS_JSR_77_FIX);
        try {
            System.setProperty(JbossUtils.NR_JBOSS_JSR_77_FIX, "true");

            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "java.util.logging,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
            System.setProperty(JbossUtils.NR_JBOSS_JSR_77_FIX, startJsr77FixVal != null ? startJsr77FixVal: "");
        }
    }

    @Test
    public void applyJbossAdjustments_when_jbossSysPkgsSet() {
        String startVal = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
        try {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, "org.junit");
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "org.junit,javax.management,java.util.logging,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
        }
    }

    @Test
    public void applyJbossAdjustments_when_jbossSysPkgsSet_and_jsr77Fix_is_true() {
        String startSysPkgVal = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
        String startJsr77FixVal = System.getProperty(JbossUtils.NR_JBOSS_JSR_77_FIX);
        try {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, "org.junit");
            System.setProperty(JbossUtils.NR_JBOSS_JSR_77_FIX, "true");
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "org.junit,java.util.logging,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.JBOSS_MODULES_SYSTEM_PKGS, startSysPkgVal != null ? startSysPkgVal: "");
            System.setProperty(JbossUtils.NR_JBOSS_JSR_77_FIX, startJsr77FixVal != null ? startJsr77FixVal: "");
        }
    }
}
