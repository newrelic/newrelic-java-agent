package com.newrelic.api.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you annotate a class with the TraceLambda annotation the New Relic agent will automatically
 * annotate matched methods within the class with a Trace annotation.
 * The TraceLambda annotation uses a regex pattern to match against method names.
 * By default the TraceLambda annotation regex pattern matches Java and Scala lambda method names.
 * The default regex pattern includes a named group called name that is used as the trace metric name.
 * By default the TraceLambda annotation does not include non-static methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceLambda {

    /**
     * Tells the agent the pattern to try matching against the marked classes method names.
     *
     * @return The pattern.
     */
    String pattern() default "";

    /**
     * Tells the agent to try matching the marked classes non-static methods against the pattern.
     *
     * @return true if non-static methods are to be included for matching
     */
    boolean includeNonstatic() default false;
}
