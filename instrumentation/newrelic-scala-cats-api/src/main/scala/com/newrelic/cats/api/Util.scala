package com.newrelic.cats.api

import cats.effect.IO
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer}

object Util {

  def wrapTrace[S](body: IO[S]): IO[S] =
    IO(AgentBridge.instrumentation.createScalaTxnTracer())
      .redeemWith(
        _ => body,
        tracer => for {
          res <- attachErrorEvent(body, tracer)
          _ <- IO(tracer.finish(172, null))
        } yield res
      )

  private def attachErrorEvent[S](body: IO[S], tracer: ExitTracer) =
    body.handleErrorWith(throwable => {
      tracer.finish(throwable)
      IO.raiseError(throwable)
    })
}
