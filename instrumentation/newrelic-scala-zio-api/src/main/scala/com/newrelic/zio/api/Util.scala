package com.newrelic.zio.api

import com.newrelic.agent.bridge.AgentBridge
import zio._

object Util {
  def wrapTrace[R, E, A](body: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.effect(AgentBridge.instrumentation.createScalaTxnTracer)
       .foldM(_ => body,
         tracer => body.bimap(
           error => {
             tracer.finish(new Throwable("ZIO txn body fail")) //error not throwable);
             error
           },
           success => {
             tracer.finish(172, null)
             success
           }))

}
