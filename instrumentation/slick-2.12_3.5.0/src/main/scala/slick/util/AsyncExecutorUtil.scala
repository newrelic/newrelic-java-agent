package slick.util

import com.newrelic.api.agent.{NewRelic, Token, Trace}
import slick.util.AsyncExecutor.PrioritizedRunnable
import slick.util.AsyncExecutor.PrioritizedRunnable.SetConnectionReleased

object AsyncExecutorUtil{

  def wrapRunMethod(run: SetConnectionReleased => Unit, token: Token): SetConnectionReleased => Unit = setConnectionReleased => {
    doRun(run, setConnectionReleased, token)
  }

  @Trace(async = true)
  def doRun(run: SetConnectionReleased => Unit, setConnectionReleased: SetConnectionReleased, token: Token): Unit = {
    if (token != null) {
      token.linkAndExpire();
    }
    NewRelic.getAgent.getTracedMethod.setMetricName("ORM", "Slick", "slickQuery")
    run(setConnectionReleased)
  }
}
