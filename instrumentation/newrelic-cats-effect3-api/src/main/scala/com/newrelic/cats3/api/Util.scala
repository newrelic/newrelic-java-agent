package com.newrelic.cats3.api

import cats.effect.Sync
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer, Token, Transaction}
import com.newrelic.api.agent.NewRelic

import java.util.concurrent.atomic.AtomicInteger

object Util {
  val RETURN_OPCODE = 176

  def wrapTrace[S, F[_] : Sync](body: TxnInfo => F[S]): F[S] =
    Sync[F].delay {
      val tracer = AgentBridge.instrumentation.createScalaTxnTracer()
      (tracer, optTxnInfo())
    }.redeemWith(
      _ => body(txnInfo),
      tracerAndTxnInfo => {
        val (tracer, optTxnInfo) = tracerAndTxnInfo
        for {
          _ <- Sync[F].delay(AgentBridge.getAgent.getTransaction(false))
          updatedTxnInfo <- setupTokenAndRefCount(optTxnInfo, txnInfo)
          res <- attachErrorEvent(body(updatedTxnInfo), tracer)
          _ <- cleanupTxnAndTokenRefCount(updatedTxnInfo)
          _ <- Sync[F].delay(tracer.finish(RETURN_OPCODE, null))
        } yield res
      }
    )

  private def txnInfo = {
    val txn = NewRelic.getAgent.getTransaction
    TxnInfo(txn, txn.getToken)
  }

  def optTxnInfo(): (Transaction, Token) = {
    val txn = AgentBridge.getAgent.getTransaction(false)
    if (txn != null) {
      (txn, txn.getToken)
    } else {
      null
    }
  }


  private def attachErrorEvent[S, F[_] : Sync](body: F[S], tracer: ExitTracer): F[S] =
    body
      .handleErrorWith(throwable => {
        tracer.finish(throwable)
        Sync[F].raiseError(throwable)
      })

  private def setupTokenAndRefCount[F[_] : Sync](optTxn: (Transaction, Token), fallback: => TxnInfo)
  : F[TxnInfo] =
    Sync[F].delay {
      if (optTxn != null) {
        val (txn, token) = optTxn
        AgentBridge.activeToken.set(
          new AgentBridge.TokenAndRefCount(
            token,
            AgentBridge.getAgent.getTracedMethod,
            new AtomicInteger(0)
          )
        )
        TxnInfo(txn, token)
      } else {
        fallback
      }
    }

  private def cleanupTxnAndTokenRefCount[F[_] : Sync](txnInfo: TxnInfo): F[Unit] = Sync[F].delay {
    val tokenAndRefCount = AgentBridge.activeToken.get()
    if (tokenAndRefCount != null) {
      AgentBridge.activeToken.remove()
    }
    txnInfo.transaction.asInstanceOf[Transaction].expireAllTokens()
  }
}
