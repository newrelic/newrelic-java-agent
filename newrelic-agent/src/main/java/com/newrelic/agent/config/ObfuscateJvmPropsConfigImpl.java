package com.newrelic.agent.config;

import java.util.*;

public class ObfuscateJvmPropsConfigImpl extends BaseConfig implements ObfuscateJvmPropsConfig {

    private static final String SYSTEM_PROPERTY_ROOT="newrelic.config.obfuscate_jvm_props";
    private static final String ALLOW = "allow";
    private static final String BLOCK = "block";
    private static final String ENABLED = "enabled";
    private static final boolean DEFAULT_ENABLED = true;
    private static final List<String> DEFAULT_BLOCK = new ArrayList<>();

    private static final Set<String> DEFAULT_ALLOW = new HashSet<>();

    static {
        DEFAULT_ALLOW.add("-javaagent");
    }

    private final boolean isEnabled;
    private final List<String> blockList;

    private final Set<String> allowList;


    public ObfuscateJvmPropsConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        blockList = initializeBlock();
        allowList = initializeAllow();

    }
    @Override
    public List<String> getBlockedJvmProps() {
        return blockList;
    }

    @Override
    public Set<String> getAllowedJvmProps() {
        return allowList;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    private List<String> initializeBlock() {
        List<String> tempObfuscated = new ArrayList<>(getUniqueStrings(BLOCK));
        tempObfuscated.addAll(DEFAULT_BLOCK);
        return tempObfuscated;
    }

    private Set<String> initializeAllow() {
        Set<String> tempUnobfuscated = new HashSet<>(getUniqueStrings(ALLOW));
        tempUnobfuscated.addAll(DEFAULT_ALLOW);
        return tempUnobfuscated;
    }
}
