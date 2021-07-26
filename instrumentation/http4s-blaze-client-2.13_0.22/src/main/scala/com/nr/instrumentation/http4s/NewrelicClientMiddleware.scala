package com.nr.instrumentation.http4s

import cats.effect.{ConcurrentEffect, Resource, Sync}
import org.http4s.Request
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.HttpParameters
import org.http4s.client.Client

import java.net.URI

object NewrelicClientMiddleware {
  def construct[F[_] : Sync, T](t: T): F[T] = Sync[F].delay(t)

  def clientResource[F[_] : ConcurrentEffect](client: Client[F]): Client[F] =
    Client { req: Request[F] =>
      for {
        seg <- Resource.eval(
          construct {
          val txn = AgentBridge.getAgent.getTransaction
          val segment = txn.startSegment("HTTP4S client call")
          segment.addOutboundRequestHeaders(new OutboundRequestWrapper(req))
          segment
        })
        response <- client.run(req)
        newRes <- Resource.eval(
          ConcurrentEffect[F].handleErrorWith
          (construct {
            seg.reportAsExternal(HttpParameters
              .library("HTTP4S")
              .uri(new URI(req.uri.toString()))
              .procedure(req.method.toString())
              .inboundHeaders(new InboundResponseWrapper(response))
              .build())
            seg.end()
            response
          })(_ => construct(response))
        )
      } yield newRes
    }

  def resource[F[_] : ConcurrentEffect](delegate: Resource[F, Client[F]]): Resource[F, Client[F]] = {
    val res: Resource[F, Client[F]] = delegate.map(c =>clientResource(c))
    res
  }
}

