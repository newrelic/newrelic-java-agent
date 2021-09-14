package com.newrelic.cats.api

import cats.effect.Sync
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer}
import com.newrelic.api.agent.NewRelic

import java.util.logging.Level

object Util {

  def wrapTrace[S, F[_]: Sync](body: F[S]): F[S] =
    Sync[F].delay{
      val tracer = AgentBridge.instrumentation.createScalaTxnTracer()
      logThreadTxnInfo()
      tracer
    }.redeemWith(
        _ => body,
        tracer => for {
          _ <- Sync[F].delay(NewRelic.getAgent.getTransaction)
          res <- attachErrorEvent(body, tracer)
          _ <- Sync[F].delay(tracer.finish(172, null))
        } yield res
      )

  private def attachErrorEvent[S, F[_]: Sync](body: F[S], tracer: ExitTracer): F[S] =
    body
      .handleErrorWith(throwable => {
      tracer.finish(throwable)
      Sync[F].raiseError(throwable)
    })


  private def logThreadTxnInfo(): Unit = {
    if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST)) {
      val txn = AgentBridge.getAgent.getTransaction(false)
      AgentBridge.getAgent.getLogger.log(Level.FINEST, s"${Thread.currentThread().getName}: txn info $txn")
    }
  }
}
