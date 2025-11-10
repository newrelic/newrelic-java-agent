package com.nr.instrumentation.http4s

import cats.data.Kleisli
import cats.effect.Sync
import com.newrelic.api.agent.{NewRelic, Token, Transaction, TransactionNamePriority}
import org.http4s.{Request, Response}
import cats.implicits._
import com.newrelic.agent.bridge.{AgentBridge, ExitTracer}

object TransactionMiddleware {
  def genHttpApp[F[_] : Sync](httpApp: Kleisli[F, Request[F], Response[F]]): Kleisli[F, Request[F], Response[F]] =
    Kleisli { req: Request[F] => nrRequestResponse(req, httpApp) }

  def nrRequestResponse[F[_] : Sync](request: Request[F], httpApp: Kleisli[F, Request[F], Response[F]]): F[Response[F]] =
    construct(AgentBridge.instrumentation.createScalaTxnTracer())
      .redeemWith(_ => httpApp(request),
             tracer => for {
               txn <- construct(NewRelic.getAgent().getTransaction())
               token <-  setupTxn(txn, request)
               res <- attachErrorEvent(httpApp(request), tracer, token)
               _ <- completeTxn(tracer, token, res, txn)
             } yield res
           )

  private def setupTxn[F[_]:Sync](txn: Transaction, request: Request[F]): F[Token] = construct {
    val token = txn.getToken
    txn.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "HTTP4s", "EmberServerHandler")
    txn.getTracedMethod.setMetricName("HTTP4s", "RequestHandler")
    txn.setWebRequest(RequestWrapper(request))
    token
  }

  private def completeTxn[F[_]:Sync](tracer: ExitTracer, token: Token, res: Response[F], txn: Transaction): F[Unit] = construct {
    txn.setWebResponse(ResponseWrapper(res))
    expireTokenIfNecessary(token)
    tracer.finish(176, null)
  }.handleErrorWith(_ => Sync[F].unit)

  private def construct[F[_]: Sync, T](t: => T): F[T] = Sync[F].delay(t)

  private def attachErrorEvent[S, F[_]: Sync](body: F[S], tracer: ExitTracer, token: Token) =
    body.handleErrorWith(throwable => {
      expireTokenIfNecessary(token)
      tracer.finish(throwable)
      Sync[F].raiseError(throwable)
    })

  private def expireTokenIfNecessary(token: Token): Unit =
    if (AgentBridge.activeToken.get() == null && token.isActive)
      token.expire()
}
