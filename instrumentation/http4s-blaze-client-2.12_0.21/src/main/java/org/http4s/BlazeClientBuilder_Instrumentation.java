package org.http4s;

import cats.effect.ConcurrentEffect;
import cats.effect.Resource;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.http4s.NewrelicClientMiddleware$;
import org.http4s.client.Client;

@Weave(type = MatchType.ExactClass, originalName = "org.http4s.client.blaze.BlazeClientBuilder")
public abstract class BlazeClientBuilder_Instrumentation<F> {

  public ConcurrentEffect F() {
    return Weaver.callOriginal();
  }

  public Resource<F, Client<F>> resource() {
    Resource<F, Client<F>> delegateResource = Weaver.callOriginal();
    return NewrelicClientMiddleware$.MODULE$.resource(delegateResource, F());
  }
}
