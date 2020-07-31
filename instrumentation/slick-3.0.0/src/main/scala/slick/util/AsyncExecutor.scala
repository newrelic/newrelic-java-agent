/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package slick.util


import com.newrelic.agent.bridge.{AgentBridge, TracedMethod}
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.internal.WeavePackageType
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.newrelic.api.agent.weaver.{SkipIfPresent, Weaver}

import scala.concurrent.ExecutionContext

@ScalaWeave(`type` = ScalaMatchType.Object, originalName="slick.util.AsyncExecutor")
class WeavedAsyncExecutor {
  def apply(name: String, numThreads: Int, queueSize: Int): AsyncExecutor = {
    val original :AsyncExecutor = Weaver.callOriginal()
    new NewRelicAsyncExecutor(original)
  }
}

/*
 * This trait is present in Slick-3.2.0-M2+
 * It is used to prevent slick-3.0 from loading with slick-3.2
 */
@SkipIfPresent
trait AsyncExecutorMXBean {
}

class NewRelicAsyncExecutor(delegatee :AsyncExecutor) extends AsyncExecutor {
  lazy val executionContext = {
    val ctx = new NewRelicExecutionContext(delegatee.executionContext)
    try {
      AgentBridge.instrumentation.retransformUninstrumentedClass(classOf[NewRelicRunnable]);
    } catch {
      case t :Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle())
      }
      case _ => ;
    }
    ctx
  }

  override def close(): Unit = {
    delegatee.close()
  }
}

class NewRelicExecutionContext(delegatee :ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = {
    try {
      AgentBridge.currentApiSource.set(WeavePackageType.INTERNAL)

      if (null != AgentBridge.getAgent().getTransaction(false) && AgentBridge.getAgent().getTransaction().isStarted()) {
        AgentBridge.getAgent().getTransaction().registerAsyncActivity(runnable)
        delegatee.execute(new NewRelicRunnable(runnable))
        return
      }
    } catch {
      case t: Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
      }
      case _ => ;
    } finally {
      AgentBridge.currentApiSource.remove();
    }
    delegatee.execute(runnable)
  }

  override def reportFailure(t: Throwable): Unit = {
    delegatee.reportFailure(t)
  }
}

class NewRelicRunnable(runnable :Runnable) extends Runnable {
  @Trace(async = true)
  override def run() {
    try {
      AgentBridge.currentApiSource.set(WeavePackageType.INTERNAL)

      if(AgentBridge.getAgent().startAsyncActivity(runnable)) {
        val tm = AgentBridge.getAgent().getTransaction().getTracedMethod().asInstanceOf[TracedMethod]
        tm.setMetricName("ORM", "Slick",  "slickQuery")
      }
    } catch {
      case t: Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
      }
      case _ => ;
    } finally {
      AgentBridge.currentApiSource.remove()
    }
    runnable.run()
  }
}
