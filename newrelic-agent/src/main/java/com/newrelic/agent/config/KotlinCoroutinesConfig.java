package com.newrelic.agent.config;

public interface KotlinCoroutinesConfig {

    public String[] getIgnoredContinuations();

    public String[] getIgnoredScopes();

    public String[] getIgnoredDispatched();

    public boolean isDelayedEnabled();

}
