package com.newrelic.zio.api;

import zio.*;
import com.newrelic.agent.bridge.AgentBridge;

public class Util {
  private Util() {
  }

  static <R, E, A> ZIO<R, E, A> wrapTrace(ZIO<R, E, A> body) {
    return ZIO$.MODULE$.effect(() -> AgentBridge.instrumentation.createScalaTxnTracer())
                       .foldM(err -> body,
                              tracer ->
                                body.bimap(error -> {
                                             tracer.finish(new Throwable("ZIO txn body fail"));//error not throwable);
                                             return error;
                                           },
                                           success -> {
                                             tracer.finish(172, null);
                                             return success;
                                           }, CanFail.canFail()), CanFail.canFail());
  }
}
