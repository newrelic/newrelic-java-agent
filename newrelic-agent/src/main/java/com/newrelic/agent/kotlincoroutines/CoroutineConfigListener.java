package com.newrelic.agent.kotlincoroutines;

public interface CoroutineConfigListener {

    void configureContinuationIgnores(String[] ignores);

    void configureScopeIgnores(String[] ignores);

    void configureDispatchedTasksIgnores(String[] ignores);

    void configureDelay(boolean enabled);
}
