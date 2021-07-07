package com.newrelic.cats.api;

import cats.effect.IO;
import cats.effect.IO$;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;

public class Util {
  private Util() {
  }

  static <S> IO<S> wrapTrace(IO<S> body) {
    return IO$.MODULE$.apply(
      () -> AgentBridge.instrumentation.createScalaTxnTracer()
    ).flatMap(
      tracer ->
        body.map(res -> {
          tracer.finish(172, null);
          return res;
        }).handleErrorWith(t -> {
          tracer.finish(t);
          return IO$.MODULE$.raiseError(t);
        })
    );
  }

}
