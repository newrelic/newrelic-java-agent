package com.newrelic.agent.config;

import java.util.List;
import java.util.Set;

public interface ObfuscateJvmPropsConfig {
    /***
     * Returns a list of JvmProps
     * @return
     */

    List<String> getBlockedJvmProps();

    Set<String> getAllowedJvmProps();

    boolean isEnabled();

}
