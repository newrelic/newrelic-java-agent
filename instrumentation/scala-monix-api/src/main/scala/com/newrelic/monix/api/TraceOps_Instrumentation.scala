package com.newrelic.monix.api

import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import monix.eval.Task


@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "com.newrelic.monix.api.TraceOps")
class TraceOps_Instrumentation {
  def txn[A](body: Task[A]): Task[A] = Util.wrapTrace(Weaver.callOriginal)
}
