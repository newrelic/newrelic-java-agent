package com.newrelic.agent.config;

import java.util.List;
import java.util.Set;

public interface ObfuscateJvmPropsConfig {

    boolean isEnabled();

    //boolean shouldObfuscate(String arg);
    boolean shouldObfuscate(String arg);
}
