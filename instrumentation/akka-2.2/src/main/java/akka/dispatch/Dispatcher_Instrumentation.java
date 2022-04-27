package akka.dispatch;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.ExecutorService;

@Weave(originalName = "akka.dispatch.Dispatcher", type = MatchType.BaseClass)
public class Dispatcher_Instrumentation {

  @Weave(originalName = "akka.dispatch.Dispatcher$LazyExecutorServiceDelegate")
  public static class LazyExecutorServiceDelegate_Instrumentation {

    public final Dispatcher $outer = Weaver.callOriginal();

    private ExecutorService executor$lzycompute() {
      ExecutorService original = Weaver.callOriginal();
      return new TokenAwareExecuter(original);
    }
  }
}
