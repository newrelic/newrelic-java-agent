package org.http4s

import cats.effect.ConcurrentEffect
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.nr.instrumentation.http4s.NewrelicClientMiddleware
import org.http4s.blaze.util.TickWheelExecutor
import org.http4s.client.{Client, Connection, ConnectionManager}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

@ScalaWeave(`type` = ScalaMatchType.Object, originalName = "org.http4s.client.blaze.BlazeClient")
class BlazeClient_Instrumentation {
  def makeClient[F[_], A <: Connection[F]](manager              : ConnectionManager[F, A],
                                           responseHeaderTimeout: Duration,
                                           idleTimeout          : Duration,
                                           requestTimeout       : Duration,
                                           scheduler            : TickWheelExecutor,
                                           ec                   : ExecutionContext)(
                                            implicit F: ConcurrentEffect[F]): Client[F] =
    NewrelicClientMiddleware.clientResource(Weaver.callOriginal())
}
