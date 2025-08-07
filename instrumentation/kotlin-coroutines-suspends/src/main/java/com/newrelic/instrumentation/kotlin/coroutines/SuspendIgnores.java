package com.newrelic.instrumentation.kotlin.coroutines;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;

public class SuspendIgnores {

    private static final List<String> ignoredSuspends = new ArrayList<String>();
    private static final String SUSPENDSIGNORECONFIG = "Coroutines.ignores.suspends";
    private static final List<String> ignoredPackages = new ArrayList<>();

    static {
        Config config = NewRelic.getAgent().getConfig();
        String value = config.getValue(SUSPENDSIGNORECONFIG);
        init(value);
        ignoredPackages.add("kotlin.coroutines");
        ignoredPackages.add("kotlinx.coroutines");
    }

    private static void init(String value) {
        if(value != null && !value.isEmpty()) {
            String[] ignores = value.split(",");
            for(String ignore : ignores) {
                addIgnore(ignore);
            }
        }
    }

    public static void reset(Config config) {
        ignoredSuspends.clear();
        String value = config.getValue(SUSPENDSIGNORECONFIG);
        init(value);
    }

    public static void addIgnore(String s) {
        if(!ignoredSuspends.contains(s)) {
            ignoredSuspends.add(s);
            NewRelic.getAgent().getLogger().log(Level.FINE, "Will ignore suspends named {0}", s);
        }
    }

    public static boolean ignoreSuspend(String className) {
        String classNameMod = className.replace('/', '.');
        int index = classNameMod.lastIndexOf('.');
        String packageName = classNameMod.substring(0, index);

        for(String ignored : ignoredPackages) {
            if(packageName.startsWith(ignored)) {
                return true;
            }
        }

        boolean classNameMatch = ignoredSuspends.contains(classNameMod);

        if(classNameMatch) {
            return true;
        }

        for(String s : ignoredSuspends) {
            Pattern pattern = Pattern.compile(s);
            Matcher matcher = pattern.matcher(classNameMod);
            if(matcher.matches()) {

                return true;
            }
        }

        return false;
    }

    public static boolean ignoreSuspend(Class<?> clazz) {

        String className = clazz.getName();
        String packageName = clazz.getPackage().getName();

        for(String ignored : ignoredPackages) {
            if(packageName.startsWith(ignored)) {
                return true;
            }
        }

        boolean classNameMatch = ignoredSuspends.contains(className);

        if(classNameMatch) {
            return true;
        }

        for(String s : ignoredSuspends) {
            Pattern pattern = Pattern.compile(s);
            Matcher matcher = pattern.matcher(className);
            if(matcher.matches()) {
                return true;
            }
        }

        return false;
    }

    public static boolean ignoreSuspend(Object obj) {
        String objString = obj.toString();
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        String packageName = clazz.getPackage().getName();

        for(String ignored : ignoredPackages) {
            if(packageName.startsWith(ignored)) {
                return true;
            }
        }

        boolean objStringMatch = ignoredSuspends.contains(objString);
        boolean classNameMatch = ignoredSuspends.contains(className);

        if(objStringMatch || classNameMatch) {
            return true;
        }

        for(String s : ignoredSuspends) {
            Pattern pattern = Pattern.compile(s);
            Matcher matcher1 = pattern.matcher(objString);
            Matcher matcher2 = pattern.matcher(className);
            if(matcher1.matches() || matcher2.matches()) return true;
        }

        return false;
    }
}