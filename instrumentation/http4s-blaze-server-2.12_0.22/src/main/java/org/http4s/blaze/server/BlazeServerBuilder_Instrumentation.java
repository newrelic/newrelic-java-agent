package org.http4s.blaze.server;

import cats.data.Kleisli;
import cats.effect.ConcurrentEffect;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.http4s.TransactionMiddleware$;
import org.http4s.Request;
import org.http4s.Response;

@Weave(originalName = "org.http4s.blaze.server.BlazeServerBuilder")
public class BlazeServerBuilder_Instrumentation<F> {
  private final ConcurrentEffect<F> F = Weaver.callOriginal();

  public BlazeServerBuilder<F> withHttpApp(Kleisli<F, Request<F>, Response<F>> httpApp) {
    httpApp = TransactionMiddleware$.MODULE$.genHttpApp(httpApp, this.F);
    return Weaver.callOriginal();
  }
}
