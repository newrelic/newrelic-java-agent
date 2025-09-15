package com.newrelic.agent.kotlincoroutines;

public interface CoroutineConfigListener {

    void configureContinuationIgnores(String[] ignores, String[] ignoresRegExs);

    void configureScopeIgnores(String[] ignores, String[] ignoresRegExs);

    void configureDispatchedTasksIgnores(String[] ignores, String[] ignoresRegExs);

    void configureDelay(boolean enabled);
}
