package com.newrelic.cats.api

import cats.effect.IO
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "com.newrelic.cats.api.TraceOps")
class TraceOps_Instrumentation {
  def txn[S](body: IO[S]): IO[S] =
     Util.wrapTrace(Weaver.callOriginal)
}
