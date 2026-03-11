package io.micronaut.web.router;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.uri.UriMatchTemplate;

@Weave(originalName = "io.micronaut.web.router.RouteMatch", type = MatchType.Interface)
public abstract class RouteMatch_instrumentation<R> {


    public abstract RouteInfo<R> getRouteInfo();

    @Trace(dispatcher = true)
    public R execute() {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "Netty", "RouteMatch", getClass().getSimpleName(), "execute");
        RouteInfo<R> routeInfo = getRouteInfo();
        if (routeInfo != null) {
            if(routeInfo instanceof UriRouteInfo<?,?>) {
                UriRouteInfo<?,?> uriRouteInfo = (UriRouteInfo<?,?>) routeInfo;
                String httpMethod = uriRouteInfo.getHttpMethodName();
                UriMatchTemplate uriTemplate = uriRouteInfo.getUriMatchTemplate();
                NewRelic.getAgent().getTracedMethod().addCustomAttribute("httpMethod", httpMethod);
                NewRelic.getAgent().getTracedMethod().addCustomAttribute("URITemplate", uriTemplate.toString());

            }
        }
        return Weaver.callOriginal();
    }
}
