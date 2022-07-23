package cats.effect.unsafe;

import cats.effect.ec.TokenAwareExecutionContext;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function0;
import scala.concurrent.ExecutionContext;

@Weave(originalName = "cats.effect.unsafe.IORuntime$")
public class IORuntime_Instrumentation {
  public IORuntime apply(ExecutionContext compute, ExecutionContext blocking, final Scheduler scheduler,
                         final Function0 shutdown, final IORuntimeConfig config) {
    compute = compute instanceof TokenAwareExecutionContext ? compute : new TokenAwareExecutionContext(compute);
    blocking = blocking instanceof TokenAwareExecutionContext ? blocking : new TokenAwareExecutionContext(blocking);
    return Weaver.callOriginal();
  }
}
