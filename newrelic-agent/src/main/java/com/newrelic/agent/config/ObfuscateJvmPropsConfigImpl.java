package com.newrelic.agent.config;

import java.util.*;

public class ObfuscateJvmPropsConfigImpl extends BaseConfig implements ObfuscateJvmPropsConfig {

    private static final String SYSTEM_PROPERTY_ROOT="newrelic.config.obfuscate_jvm_props.";
    private static final String ALLOW = "allow";
    private static final String BLOCK = "block";
    private static final String ENABLED = "enabled";
    private static final boolean DEFAULT_ENABLED = true;
    private static final Set<String> DEFAULT_ALLOW = new HashSet<>();

    static {
        //the standard and extended JVM props should be allowed through by default
        DEFAULT_ALLOW.add("-X*");
    }
    private final boolean isEnabled;
    private final Set<String> blockList;
    private final Set<String> allowList;

    public ObfuscateJvmPropsConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        blockList = initializeBlock();
        allowList = initializeAllow();
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Set<String> getAllow() { return allowList; }

    @Override
    public Set<String> getBlock() { return blockList; }

    private Set<String> initializeBlock() {
        return new HashSet<>(getUniqueStrings(BLOCK));
    }

    private Set<String> initializeAllow() {
        Set<String> tempAllow = new HashSet<>(getUniqueStrings(ALLOW));
        tempAllow.addAll(DEFAULT_ALLOW);
        return tempAllow;
    }
}
