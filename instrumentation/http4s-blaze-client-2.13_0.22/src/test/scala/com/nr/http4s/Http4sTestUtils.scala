package com.nr.http4s


import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.newrelic.agent.introspec.{Introspector, TraceSegment, TransactionTrace}
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import com.newrelic.cats.api.TraceOps._

object Http4sTestUtils {

  def makeRequest[F[_] : ContextShift : Timer](url: String)(
    implicit ex: ExecutionContext, c: ConcurrentEffect[F]): F[String] = {
    txn(
      BlazeClientBuilder[F](ex).resource.use { client =>
        client.expect[String](url)
      }
    )
  }

  def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  def getSegments(traces: Iterable[TransactionTrace]): Iterable[TraceSegment] =
    traces.flatMap(trace => this.getSegments(trace.getInitialTraceSegment))

  private def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }
}
