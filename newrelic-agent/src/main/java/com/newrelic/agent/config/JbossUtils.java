package com.newrelic.agent.config;

import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;

public class JbossUtils {

    public static final String JBOSS_MODULES_MAIN_CLASS = "org/jboss/modules/Main.class";
    public static final String JBOSS_MODULES_MAIN = "org.jboss.modules.Main";
    public static final String JBOSS_MODULES_SYSTEM_PKGS = "jboss.modules.system.pkgs";
    public static final String COM_NR_INSTRUMENTATION_SECURITY = "com.nr.instrumentation.security";
    public static final String JOIN_STR_COM_NR_INSTRUMENTATION_SECURITY = ",com.nr.instrumentation.security";

    /**
     * DO NOT call this outside of premain. This will iterate thru all loaded classes in the Instrumentation,
     * possibly causing a LOT of overhead.
     * The system property set is required by the security agent.
     */
    public static void checkAndApplyJbossAdjustments(Instrumentation inst) {
        if (isJbossServer(inst)) {
            String cur = System.getProperty(JBOSS_MODULES_SYSTEM_PKGS);
            if (StringUtils.isBlank(cur)) {
                System.setProperty(JBOSS_MODULES_SYSTEM_PKGS, COM_NR_INSTRUMENTATION_SECURITY);
            } else if (!StringUtils.containsIgnoreCase(cur, COM_NR_INSTRUMENTATION_SECURITY)) {
                System.setProperty(JBOSS_MODULES_SYSTEM_PKGS, cur + JOIN_STR_COM_NR_INSTRUMENTATION_SECURITY);
            }
        }
    }

    public static boolean isJbossServer(Instrumentation inst) {
        if (ClassLoader.getSystemClassLoader().getResource(JBOSS_MODULES_MAIN_CLASS) != null) {
            return true;
        }
        if (isClassLoaded(JBOSS_MODULES_MAIN, inst)) {
            return true;
        }
        return false;
    }

    private static boolean isClassLoaded(String className, Instrumentation instrumentation) {
        if (instrumentation == null || className == null) {
            throw new IllegalArgumentException("instrumentation and className must not be null");
        }
        Class<?>[] classes = instrumentation.getAllLoadedClasses();
        if (classes != null) {
            for (Class<?> klass : classes) {
                if (className.equals(klass.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
