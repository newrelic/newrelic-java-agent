package cats.effect.internals;

import cats.effect.Timer;
import cats.effect.ec.TokenAwareExecutionContext;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.concurrent.ExecutionContext;

@Weave(originalName = "cats.effect.internals.IOTimer$")
public class IOTimer_Instrumentation {
  public Timer apply(ExecutionContext ec) {
    ec = ec instanceof TokenAwareExecutionContext ? ec : new TokenAwareExecutionContext(ec);
    return Weaver.callOriginal();
  }
}
