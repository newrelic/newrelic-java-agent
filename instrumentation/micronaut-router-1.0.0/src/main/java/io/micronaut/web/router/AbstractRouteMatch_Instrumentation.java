package io.micronaut.web.router;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.HttpMethod;

import java.util.Map;

@Weave(originalName = "io.micronaut.web.router.AbstractRouteMatch", type = MatchType.BaseClass)
abstract class AbstractRouteMatch_Instrumentation<T,R> {

    @Trace
    public R execute(Map<String, Object> argumentValues) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "Netty", "RouteMatch", getClass().getSimpleName(), "execute");
        if(this instanceof DefaultUriRouteMatch_Instrumentation) {
            HttpMethod method = ((DefaultUriRouteMatch_Instrumentation<?, ?>) this).getHttpMethod();
            String methodName = method.name();
            String uri = ((DefaultUriRouteMatch_Instrumentation<?, ?>) this).getUri();
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "RouteMatch", methodName + " -" + uri);
        }
        return Weaver.callOriginal();
    }

}
