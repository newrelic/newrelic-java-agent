package io.micronaut.core.propagation;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Necessary to keep from instrumenting 4.3.0 and higher because instrumentation for 4.3.0 includes
 * instrumentation for new functionality
 */
@SkipIfPresent(originalName = "io.micronaut.core.propagation.ThreadContext")
public class ThreadContext_Skip {
}
