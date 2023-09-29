package com.newrelic.agent.config;

import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JbossUtils {

    public static final String JBOSS_MODULES_MAIN_CLASS = "org/jboss/modules/Main.class";
    public static final String JBOSS_MODULES_MAIN = "org.jboss.modules.Main";
    public static final String SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS = "jboss.modules.system.pkgs";
    public static final String SYS_PROP_NR_SYSTEM_PKGS_EXCLUDES = "com.newrelic.jboss.modules.system.pkgs.excludes";
    public static final String COM_NR_INSTRUMENTATION_SECURITY = "com.nr.instrumentation.security";

//    public static final String JAVA_UTIL_LOGGING = "java.util.logging";
//    public static final String JAVA_LANG_MANAGEMENT = "java.lang.management";
//    public static final String JAVAX_MANAGEMENT = "javax.management";

    /**
     * DO NOT call this outside of premain. This will iterate thru all loaded classes in the Instrumentation,
     * possibly causing a LOT of overhead.
     * The system property set is required by the APM and security agents.
     */
    public void checkAndApplyJbossAdjustments(Instrumentation inst) {
        if (isJbossServer(inst)) {
            String cur = System.getProperty(SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS);
            String jbossSystemPkgsValue = createJbossSystemPkgsValue();
            if (StringUtils.isBlank(cur)) {
                System.setProperty(SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, jbossSystemPkgsValue);
            } else if (!StringUtils.containsIgnoreCase(cur, COM_NR_INSTRUMENTATION_SECURITY)) {
                System.setProperty(SYS_PROP_JBOSS_MODULES_SYSTEM_PKGS, cur + "," + jbossSystemPkgsValue);
            }
        }
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

    private String createJbossSystemPkgsValue() {
        String[] excludedPackages = System.getProperty(SYS_PROP_NR_SYSTEM_PKGS_EXCLUDES, "").split(",");
        String[] defaultSysPackages = {
//                JAVA_UTIL_LOGGING,
//                JAVAX_MANAGEMENT,
                COM_NR_INSTRUMENTATION_SECURITY,
//                JAVA_LANG_MANAGEMENT
        };
        List<String> systemPkgs = new ArrayList<>(defaultSysPackages.length);
        for (String pkg: defaultSysPackages) {
            // Check if the package is to be excluded. If so, continue to the next element in defaultSysPackages.
            if (arrayContains(excludedPackages, pkg)) {
                continue;
            }
            systemPkgs.add(pkg);
        }
        return String.join(",", systemPkgs);
    }

    private boolean arrayContains(String[] array, String value) {
        for (String elem: array) {
            if (elem.equals(value)) {
                return true;
            }
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
