package com.newrelic.zio2.api

import com.newrelic.agent.bridge.AgentBridge
import zio._

object Util {
  def wrapTrace[R, E, A](body: ZIO[R, E, A]): ZIO[R, E, A] = {
    ZIO.attempt(AgentBridge.instrumentation.createScalaTxnTracer)
       .foldZIO(_ => body,
         tracer => if (tracer == null) {
           body
         } else {
           body.mapBoth(
             error => {
               AgentBridge.activeToken.remove()
               tracer.finish(new Throwable("ZIO txn body fail"))
               error
             },
             success => {
               AgentBridge.activeToken.remove()
               tracer.finish(172, null)
               success
             })
         })
  }
}
