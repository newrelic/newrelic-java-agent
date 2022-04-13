package akka.dispatch;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function0;
import scala.concurrent.duration.Duration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

@Weave(originalName = "akka.dispatch.ThreadPoolConfig")
public class ThreadPoolConfig_Instrumentation {

  @Weave(originalName = "akka.dispatch.ThreadPoolConfig$ThreadPoolExecutorServiceFactory")
  public static class ThreadPoolExecutorServiceFactory_Instrumentation {
    private final ThreadFactory threadFactory = Weaver.callOriginal();

    public ExecutorService createExecutorService() {
      ExecutorService original = Weaver.callOriginal();
      TokenAwareThreadPoolExecuter service = new TokenAwareThreadPoolExecuter(original);
      return service;
    }
  }
}
