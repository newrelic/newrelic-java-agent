package zio;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Boolean;
import scala.Function0;
import java.time.Duration;
import zio.Unsafe;

@Weave(originalName = "zio.Scheduler", type = MatchType.BaseClass)
public class ZIOScheduler_Instrumentation {

  public Function0<Boolean> schedule(Runnable task, Duration duration, Unsafe unsafe) {
    task = new TokenAwareRunnable(task);
    return Weaver.callOriginal();
  }
}
