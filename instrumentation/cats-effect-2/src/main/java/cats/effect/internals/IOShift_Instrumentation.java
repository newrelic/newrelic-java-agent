package cats.effect.internals;

import cats.effect.IO;
import cats.effect.ec.TokenAwareExecutionContext;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Unit;
import scala.concurrent.ExecutionContext;


@Weave(originalName = "cats.effect.internals.IOShift$")
public class IOShift_Instrumentation {
  public IO apply(ExecutionContext ec) {
    ec = ec instanceof TokenAwareExecutionContext ? ec : new TokenAwareExecutionContext(ec);
    return Weaver.callOriginal();
  }
}
