package com.newrelic.monix.api

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.NewRelic
import monix.eval.Task

object Util {
  val RETURN_OPCODE = 176
  def wrapTrace[A](body: Task[A]): Task[A] =
    Task.delay({
      AgentBridge.instrumentation.createScalaTxnTracer
    }).redeemWith(
      _ => body,
      tracer => if(tracer == null) {
        body
      } else {
        for {
          _ <- Task.delay(NewRelic.getAgent.getTransaction)
          res <- body.onErrorHandleWith(throwable => {
            tracer.finish(throwable)
            Task.raiseError(throwable)
          })
          _ <- Task.delay(tracer.finish(RETURN_OPCODE, null))
        } yield res
      }
    )
}