package com.newrelic.api.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * A class annotated with TraceByReturnType will cause the New Relic agent to annotate methods
 * with a return type matching Classes specified in {@link TraceByReturnType#traceReturnTypes}
 * with a @{link com.newrelic.api.agent.Trace} annotation.
 */

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceByReturnType {

  /**
   * Specifies return types of methods that should be annotated with {@link TraceByReturnType#traceReturnTypes}
   * Type parameters for Generic Types are ignored. e.g. List.class will match methods returning List<Integer> and
   * List<String>
   */
  Class[] traceReturnTypes() default {};
}
