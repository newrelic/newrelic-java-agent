package zio;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Boolean;
import scala.Function0;
import zio.internal.TokenAwareRunnable;

import java.time.Duration;

@Weave(originalName = "zio.Scheduler", type = MatchType.BaseClass)
public class ZIOScheduler_Instrumentation {

  public Function0<Boolean> schedule(Runnable task, Duration duration) {
    task = new TokenAwareRunnable(task);
    return Weaver.callOriginal();
  }
}
