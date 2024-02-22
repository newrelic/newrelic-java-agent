package com.newrelic.zio2.api

import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import zio.ZIO


@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "com.newrelic.zio2.api.TraceOps")
class ZIOTraceOps_Instrumentation {
  def txn[R, E, A](body: ZIO[R, E, A]): ZIO[R, E, A] =  Util.wrapTrace(Weaver.callOriginal)
}
