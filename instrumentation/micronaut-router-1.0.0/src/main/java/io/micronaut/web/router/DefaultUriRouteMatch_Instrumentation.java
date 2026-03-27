package io.micronaut.web.router;

import com.newrelic.api.agent.weaver.Weave;
import io.micronaut.http.HttpMethod;

@Weave(originalName = "io.micronaut.web.router.DefaultUriRouteMatch")
abstract class DefaultUriRouteMatch_Instrumentation<T,R> extends AbstractRouteMatch_Instrumentation<T,R> {

    public abstract String getUri();
    public abstract HttpMethod getHttpMethod();
}
