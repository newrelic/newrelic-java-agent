package com.newrelic.agent.service.module;

import java.net.URL;

public interface ClassToJarPathSubmitter {

    /**
     * Determines if the supplied URL maps to a jar-type code location and if so, submits
     * the jar to the jar collector analysis queue.
     *
     * @param url the URL to analyse
     */
    void processUrl(URL url);
}
