package org.http4s;

import cats.effect.kernel.Async;
import cats.effect.kernel.Resource;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.http4s.NewrelicClientMiddleware$;
import org.http4s.client.Client;

@Weave(type = MatchType.ExactClass, originalName = "org.http4s.ember.client.EmberClientBuilder")
public abstract class EmberClientBuilder_Instrumentation<F> {

  private final Async<F> evidence$1 = Weaver.callOriginal();
  public Resource<F, Client<F>> build() {
    Resource<F, Client<F>> delegateResource = Weaver.callOriginal();
    return NewrelicClientMiddleware$.MODULE$.resource(delegateResource, evidence$1);
  }
}
