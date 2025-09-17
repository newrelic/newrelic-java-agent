package com.newrelic.agent.kotlincoroutines;

public interface SuspendsConfigListener {

    void configureSuspendsIgnores(String[] ignores, String[] ignoresRegex);
}
