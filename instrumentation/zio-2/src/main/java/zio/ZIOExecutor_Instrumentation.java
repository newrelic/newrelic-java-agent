package zio;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import zio.Unsafe;

@Weave(originalName = "zio.Executor", type = MatchType.BaseClass)
public class ZIOExecutor_Instrumentation {

  public boolean submit(Runnable runnable, Unsafe unsafe) {
    runnable = new TokenAwareRunnable(runnable);
    return Weaver.callOriginal();
  }
}
