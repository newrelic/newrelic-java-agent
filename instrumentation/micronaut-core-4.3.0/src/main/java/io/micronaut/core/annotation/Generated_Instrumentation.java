package io.micronaut.core.annotation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;

@WeaveWithAnnotation(annotationClasses = {"io.micronaut.core.annotation.Generated"}, type = MatchType.Interface)
public abstract class Generated_Instrumentation {

    @WeaveWithAnnotation(annotationClasses = {"io.micronaut.core.annotation.Generated"})
    @WeaveIntoAllMethods
    @Trace
    private static void instrumentation() {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        StackTraceElement firstElement = traces[1];
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "Generated", firstElement.getMethodName(), firstElement.getMethodName());
    }
}