package com.newrelic.agent.config;

import java.util.Set;

public interface ObfuscateJvmPropsConfig {

    boolean isEnabled();

    Set<String> getAllow();

    Set<String> getBlock();
}
