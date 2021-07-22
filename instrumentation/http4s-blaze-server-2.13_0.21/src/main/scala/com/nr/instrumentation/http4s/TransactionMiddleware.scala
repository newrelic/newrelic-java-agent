package com.nr.instrumentation.http4s

import cats.data.Kleisli
import cats.effect.Sync
import com.newrelic.api.agent.Token
import org.http4s.{Request, Response}
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer, Transaction, TransactionNamePriority}

object TransactionMiddleware {
  def genHttpApp[F[_] : Sync](httpApp: Kleisli[F, Request[F], Response[F]]): Kleisli[F, Request[F], Response[F]] =
    Kleisli { req: Request[F] => nrRequestResponse(req, httpApp) }

  def nrRequestResponse[F[_] : Sync](request: Request[F], httpAp: Kleisli[F, Request[F], Response[F]]): F[Response[F]] =
    construct(AgentBridge.instrumentation.createScalaTxnTracer())
      .redeemWith(_ => httpAp(request),
        tracer => for {
          txn <- construct(AgentBridge.getAgent.getTransaction)
          token <- setupTxn(txn, request)
          res <- attachErrorEvent(httpAp(request), tracer, token)
          _ <- completeTxn(tracer, token)
        } yield res
      )

  private def setupTxn[F[_] : Sync](txn: Transaction, request: Request[F]): F[Token] = construct {
    val t = txn.asInstanceOf[com.newrelic.api.agent.Transaction]
    val token = t.getToken
    txn.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, "HTTP4s", "BlazeServerHandler")
    txn.getTracedMethod.setMetricName("HTTP4s", "RequestHandler")
    t.setWebRequest(RequestWrapper(request))
    token
  }

  private def completeTxn[F[_] : Sync](tracer: ExitTracer, token: Token): F[Unit] = construct {
    token.expire()
    tracer.finish(176, null)
  }

  private def construct[F[_] : Sync, T](t: T): F[T] = Sync[F].delay(t)

  private def attachErrorEvent[S, F[_] : Sync](body: F[S], tracer: ExitTracer, token: Token) =
    body.handleErrorWith(throwable => {
      token.expire()
      tracer.finish(throwable)
      Sync[F].raiseError(throwable)
    })

}

