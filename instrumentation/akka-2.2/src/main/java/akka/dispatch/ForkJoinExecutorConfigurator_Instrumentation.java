package akka.dispatch;

import akka.dispatch.forkjoin.ForkJoinPool;
import akka.dispatch.forkjoin.ForkJoinTask;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "akka.dispatch.ForkJoinExecutorConfigurator")
public class ForkJoinExecutorConfigurator_Instrumentation {

  @Weave(originalName = "akka.dispatch.ForkJoinExecutorConfigurator$AkkaForkJoinPool")

  public static final class AkkaForkJoinPool_Instrumentation {
    public void execute(Runnable runnable) {
      if (runnable != null) {
        runnable = new TokenAwareRunnable(runnable);
      }
      Weaver.callOriginal();
    }
  }
}
