package com.newrelic.agent.config;

import com.newrelic.agent.attributes.ExcludeIncludeFilter;
import com.newrelic.agent.attributes.ExcludeIncludeFilterImpl;

import java.util.*;

public class ObfuscateJvmPropsConfigImpl extends BaseConfig implements ObfuscateJvmPropsConfig {

    private static final String SYSTEM_PROPERTY_ROOT="newrelic.config.obfuscate_jvm_props.";
    private static final String ALLOW = "allow";
    private static final String BLOCK = "block";
    private static final String ENABLED = "enabled";
    private static final boolean DEFAULT_ENABLED = true;
    private static final Set<String> DEFAULT_ALLOW = new HashSet<>();

    static {
        DEFAULT_ALLOW.add("-XX*");
        DEFAULT_ALLOW.add("-X*");
    }
    private final boolean isEnabled;
    private final Set<String> blockList;
    private final Set<String> allowList;
    private final ExcludeIncludeFilter filter;

    public ObfuscateJvmPropsConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        blockList = initializeBlock();
        allowList = initializeAllow();
        filter = new ExcludeIncludeFilterImpl("obfuscate_jvm_props", blockList, allowList, false);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean shouldObfuscate(String prop) {
        return !filter.shouldInclude(prop);
    }

    private Set<String> initializeBlock() {
        return new HashSet<>(getUniqueStrings(BLOCK));
    }

    private Set<String> initializeAllow() {
        Set<String> tempAllow = new HashSet<>(getUniqueStrings(ALLOW));
        tempAllow.addAll(DEFAULT_ALLOW);
        return tempAllow;
    }
}
