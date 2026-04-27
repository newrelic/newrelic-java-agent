package io.micronaut.http.filter;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;

@Weave(originalName = "io.micronaut.http.filter.ServerFilterChain", type = MatchType.Interface)
public class ServerFilterChain_Instrumentation {

    @Trace
    public Publisher<MutableHttpResponse<?>> proceed(HttpRequest<?> request) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","ServerFilterChain", getClass().getSimpleName(),"proceed");
        return Weaver.callOriginal();
    }

}
