package cats.effect.ec

import com.newrelic.agent.bridge.AgentBridge

import java.util.logging.Level
import scala.concurrent.ExecutionContext

class TokenAwareExecutionContext(delegate: ExecutionContext) extends ExecutionContext  {
  override def execute(runnable: Runnable): Unit = {
    delegate.execute(new TokenAwareRunnable(runnable))
  }
  override def reportFailure(cause: Throwable): Unit = delegate.reportFailure(cause)
}