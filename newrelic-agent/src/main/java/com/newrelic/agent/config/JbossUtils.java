package com.newrelic.agent.config;

import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;

public class JbossUtils {

    // Used to check if JBoss modules is used
    public static final String JBOSS_MODULES_MAIN_CLASS = "org/jboss/modules/Main.class";
    public static final String JBOSS_MODULES_MAIN = "org.jboss.modules.Main";

    // System properties
    public static final String NR_JBOSS_JSR_77_FIX = "com.newrelic.jboss.jsr77.fix";
    public static final String JBOSS_MODULES_SYSTEM_PKGS = "jboss.modules.system.pkgs";

    // Java packages
    public static final String COM_NR_INSTRUMENTATION_SECURITY = "com.nr.instrumentation.security";
    public static final String JAVA_UTIL_LOGGING = "java.util.logging";
    public static final String JAVA_LANG_MANAGEMENT = "java.lang.management";
    public static final String JAVAX_MANAGEMENT = "javax.management";

    /**
     * DO NOT call this outside of premain. This will iterate thru all loaded classes in the Instrumentation,
     * possibly causing a LOT of overhead.
     * The system property set is required by the APM and security agents.
     */
    public void checkAndApplyJbossAdjustments(Instrumentation inst) {
        if (isJbossServer(inst)) {
            String cur = System.getProperty(JBOSS_MODULES_SYSTEM_PKGS);
            if (StringUtils.isBlank(cur)) {
                System.setProperty(JBOSS_MODULES_SYSTEM_PKGS, getJbossSystemPackages());
            } else if (!StringUtils.containsIgnoreCase(cur, COM_NR_INSTRUMENTATION_SECURITY)) {
                System.setProperty(JBOSS_MODULES_SYSTEM_PKGS, cur + "," + getJbossSystemPackages());
            }
        }
    }

    public String getJbossSystemPackages() {
        String defaultJbossSysPackages = String.join(",", JAVA_UTIL_LOGGING, COM_NR_INSTRUMENTATION_SECURITY, JAVA_LANG_MANAGEMENT);
        String jsr77FixProp = System.getProperty(NR_JBOSS_JSR_77_FIX);
        if (Boolean.TRUE.toString().equalsIgnoreCase(jsr77FixProp)) {
            return defaultJbossSysPackages;
        }
        return JAVAX_MANAGEMENT + "," + defaultJbossSysPackages;
    }

    public boolean isJbossServer(Instrumentation inst) {
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
