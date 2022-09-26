package cats.effect.internals;

import cats.effect.Timer;
import com.nr.agent.instrumentation.TokenAwareExecutionContext;
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
