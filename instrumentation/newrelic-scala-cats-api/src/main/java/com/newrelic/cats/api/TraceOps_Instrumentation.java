package com.newrelic.cats.api;

import cats.effect.IO;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.newrelic.cats.api.TraceOps$")
public class TraceOps_Instrumentation {
  public <S> IO<S> txn(final IO<S> body) {
    return Util.wrapTrace(Weaver.callOriginal());
  }
}
