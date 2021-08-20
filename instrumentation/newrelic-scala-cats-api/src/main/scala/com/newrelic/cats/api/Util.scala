package com.newrelic.cats.api

import cats.effect.Sync
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer}
import com.newrelic.api.agent.NewRelic

object Util {

  def wrapTrace[S, F[_]: Sync](body: F[S]): F[S] =
    Sync[F].delay(AgentBridge.instrumentation.createScalaTxnTracer())
      .redeemWith(
        _ => body,
        tracer => for {
          txn <- Sync[F].delay(NewRelic.getAgent.getTransaction)
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
}
