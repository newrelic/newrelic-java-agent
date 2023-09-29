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
        String startVal = System.getProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
        try {
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "java.util.logging,javax.management,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
        }
    }

    @Test
    public void checkAndApplyJbossAdjustments_when_sysPropSet() {
        String startVal = System.getProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
        try {
            System.setProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, "org.junit");
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "org.junit,java.util.logging,javax.management,com.nr.instrumentation.security,java.lang.management";
            String actual = System.getProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, startVal != null ? startVal: "");
        }
    }

    @Test
    public void checkAndApplyJbossAdjustments_when_sysPropSet_and_excludesSet() {
        String startValSysPkgs = System.getProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
        String startValExcludes = System.getProperty(JbossUtils.SYS_PROP_NR_SYSTEM_PKGS_EXCLUDES);

        try {
            System.setProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, "org.junit");
            System.setProperty(JbossUtils.SYS_PROP_NR_SYSTEM_PKGS_EXCLUDES, "javax.management,com.nr.instrumentation.security");
            Instrumentation inst = Mockito.mock(Instrumentation.class);

            JbossUtils utils = Mockito.spy(new JbossUtils());
            Mockito.when(utils.isJbossServer(inst)).thenReturn(true);

            utils.checkAndApplyJbossAdjustments(inst);

            String expectedSysProp = "org.junit,java.util.logging,java.lang.management";
            String actual = System.getProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
            Assert.assertEquals(expectedSysProp, actual);
        } finally {
            System.setProperty(JbossUtils.SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, startValSysPkgs != null ? startValSysPkgs: "");
            System.setProperty(JbossUtils.SYS_PROP_NR_SYSTEM_PKGS_EXCLUDES, startValExcludes != null ? startValExcludes: "");

        }
    }
}
