package com.newrelic.agent.config;

import java.lang.instrument.Instrumentation;

public class JbossUtils {

    public static final String JBOSS_MODULES_MAIN_CLASS = "org/jboss/modules/Main.class";
    public static final String JBOSS_MODULES_MAIN = "org.jboss.modules.Main";
    public static final String JBOSS_MODULES_SYSTEM_PKGS = "jboss.modules.system.pkgs";
    public static final String COM_NR_INSTRUMENTATION_SECURITY = "com.nr.instrumentation.security";
    public static final String JOIN_STR_COM_NR_INSTRUMENTATION_SECURITY = ",com.nr.instrumentation.security";

    public static boolean isJbossServer(Instrumentation inst) {
        if (ClassLoader.getSystemClassLoader().getResource(JBOSS_MODULES_MAIN_CLASS) != null) {
            return true;
        }
        if (isClassLoaded(JbossUtils.JBOSS_MODULES_MAIN, inst)) {
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
