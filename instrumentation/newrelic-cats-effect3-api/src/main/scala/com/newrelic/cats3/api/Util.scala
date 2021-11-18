package com.newrelic.cats3.api

import cats.effect.Sync
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer, Transaction, TracedMethod}

import java.util.concurrent.atomic.AtomicInteger

object Util {
  val RETURN_OPCODE = 176

  def wrapTrace[S, F[_] : Sync](body: F[S]): F[S] =
    Sync[F].delay(AgentBridge.instrumentation.createScalaTxnTracer())
           .redeemWith(_ => body,
             tracer =>
               if (tracer == null) {
                 body
               } else {
                 for {
                   txnWithTracedMethod <- Sync[F].delay {
                     val agent = AgentBridge.getAgent
                     (agent.getTransaction(false), agent.getTracedMethod)
                   }
                   _ <- setupTokenAndRefCount(txnWithTracedMethod)
                   res <- attachErrorEvent(body, tracer)
                   _ <- cleanupTxnAndTokenRefCount(txnWithTracedMethod._1)
                   _ <- Sync[F].delay(tracer.finish(RETURN_OPCODE, null))
                 } yield res
               }
           )

  private def attachErrorEvent[S, F[_] : Sync](body: F[S], tracer: ExitTracer): F[S] =
    body
      .handleErrorWith(throwable => {
        tracer.finish(throwable)
        Sync[F].raiseError(throwable)
      })

  private def setupTokenAndRefCount[F[_] : Sync](txnWithTracerMethod: (Transaction, TracedMethod)): F[Unit] = Sync[F].delay {

    val (txn, tracedMethod) = txnWithTracerMethod
    if (txn != null && tracedMethod != null) {
      AgentBridge.activeToken.set(
        new AgentBridge.TokenAndRefCount(
          txn.getToken,
          tracedMethod,
          new AtomicInteger(0)
        ))
    }
  }

  private def cleanupTxnAndTokenRefCount[F[_] : Sync](txn: Transaction): F[Unit] = Sync[F].delay {
    AgentBridge.activeToken.remove()
    if (txn != null) {
      txn.expireAllTokens()
    }
  }
}
