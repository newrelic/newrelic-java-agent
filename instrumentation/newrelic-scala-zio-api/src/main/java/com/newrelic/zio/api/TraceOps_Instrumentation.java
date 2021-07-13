package com.newrelic.zio.api;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import zio.ZIO;

@Weave(originalName = "com.newrelic.zio.api.TraceOps$")
public class TraceOps_Instrumentation {
  public <R, E, A> ZIO<R, E, A> txn(final ZIO<R, E, A> body) {
    return Util.wrapTrace(Weaver.callOriginal());
  }
}
