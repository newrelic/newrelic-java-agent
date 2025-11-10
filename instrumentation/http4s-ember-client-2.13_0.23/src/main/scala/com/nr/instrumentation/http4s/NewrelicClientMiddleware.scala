package com.nr.instrumentation.http4s

import cats.effect.{Resource, Sync}
import cats.effect.kernel.Async
import org.http4s.Request
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.HttpParameters
import org.http4s.client.Client

import java.net.URI
import java.util.logging.Level

object NewrelicClientMiddleware {
  def construct[F[_] : Sync, T](t: T): F[T] = Sync[F].delay(t)

  def clientResource[F[_] : Async](client: Client[F]): Client[F] =
    Client { req: Request[F] =>
      for {
        seg <- Resource.eval(
          construct {
          val txn = AgentBridge.getAgent.getTransaction
          logTokenInfo(AgentBridge.activeToken.get, s"client call for txn $txn")
          val segment = txn.startSegment("HTTP4S client call")
          segment.addOutboundRequestHeaders(new OutboundRequestWrapper(req))
          segment
        })
        response <- client.run(req)
        newRes <- Resource.eval(
          Async[F].handleErrorWith
          (construct {
            seg.reportAsExternal(HttpParameters
              .library("HTTP4S")
              .uri(new URI(req.uri.toString()))
              .procedure(req.method.toString())
              .inboundHeaders(new InboundResponseWrapper(response.headers))
              .build())
            seg.end()
            response
          })(_ => construct(response))
        )
      } yield newRes
    }

  def resource[F[_] : Async](delegate: Resource[F, Client[F]]): Resource[F, Client[F]] = {
    val res: Resource[F, Client[F]] = delegate.map(c =>clientResource(c))
    res
  }

  def logTokenInfo(tokenAndRefCount: AgentBridge.TokenAndRefCount, msg: String): Unit = {
    if (AgentBridge.getAgent.getLogger.isLoggable(Level.FINEST)) {
      val tokenMsg = if (tokenAndRefCount != null && tokenAndRefCount.token != null) s"[${tokenAndRefCount.token}:${tokenAndRefCount.token.getTransaction}:${tokenAndRefCount.refCount.get}]"
      else "[Empty token]"
      AgentBridge.getAgent.getLogger.log(Level.FINEST,
        s"[${Thread.currentThread().getName}] ${tokenMsg}: [Http4s ember client] token info ${msg}")
    }
  }
}

