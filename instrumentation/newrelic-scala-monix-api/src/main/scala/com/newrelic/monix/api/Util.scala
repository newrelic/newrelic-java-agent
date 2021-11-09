package com.newrelic.monix.api

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.NewRelic
import monix.eval.Task

object Util {
  def wrapTrace[A](body: Task[A]): Task[A] =
    Task.delay({
      AgentBridge.instrumentation.createScalaTxnTracer
    }).redeemWith(
      _ => body,
      tracer => for {
        _ <- Task.delay(NewRelic.getAgent.getTransaction)
        res <- body.onErrorHandleWith(throwable => {
          tracer.finish(throwable)
          Task.raiseError(throwable)
        })
        _ <- Task.delay(tracer.finish(172, null))
      } yield res
    )
}