package zio.internal;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "zio.internal.Executor", type = MatchType.BaseClass)
public class ZIOExecutor_Instrumentation {

  public boolean submit(Runnable runnable) {
    runnable = new TokenAwareRunnable(runnable);
    return Weaver.callOriginal();
  }
}
