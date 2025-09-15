package com.newrelic.agent.config;

public interface KotlinCoroutinesConfig {

    public String[] getIgnoredContinuations();

    public String[] getIgnoredRegExContinuations();

    public String[] getIgnoredScopes();

    public String[] getIgnoredRegexScopes();

    public String[] getIgnoredDispatched();

    public String[] getIgnoredRegexDispatched();

    public boolean isDelayedEnabled();

}
