package zio;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.agent.instrumentation.zio2.TokenAwareRunnable;
import scala.Boolean;
import scala.Function0;

import java.time.Duration;

@Weave(originalName = "zio.internal.Scheduler", type = MatchType.BaseClass)
public class ZIOScheduler_Instrumentation {

  public Function0<Boolean> schedule(Runnable task, Duration duration) {
    task = new TokenAwareRunnable(task);
    return Weaver.callOriginal();
  }
}
