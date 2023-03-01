package com.newrelic.agent.util;

import java.lang.instrument.Instrumentation;

public interface UnwindableInstrumentation extends Instrumentation {
    /**
     * Remove all New Relic class transformers and rejit any classes they have modified
     */
    void unwind();

    void started();
}
