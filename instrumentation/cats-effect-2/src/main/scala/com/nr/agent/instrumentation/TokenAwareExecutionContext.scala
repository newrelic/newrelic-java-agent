package com.nr.agent.instrumentation

import com.newrelic.agent.bridge.AgentBridge

import java.util.logging.Level
import scala.concurrent.ExecutionContext

class TokenAwareExecutionContext(delegate: ExecutionContext) extends ExecutionContext  {
  AgentBridge.getAgent.getLogger.log(Level.INFO, s"[${Thread.currentThread().getName}] Instrumenting IOShift " +
    s"ExecutionContext $delegate")
  override def execute(runnable: Runnable): Unit = {
    delegate.execute(new TokenAwareRunnable(runnable))
  }
  override def reportFailure(cause: Throwable): Unit = delegate.reportFailure(cause)
}