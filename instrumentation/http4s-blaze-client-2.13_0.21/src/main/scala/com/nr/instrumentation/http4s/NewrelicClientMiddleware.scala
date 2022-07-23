package com.nr.instrumentation.http4s

import cats.effect.{ConcurrentEffect, Resource, Sync}
import org.http4s.{Request, Response}
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.{HttpParameters, Segment}
import org.http4s.client.Client

import java.net.URI

object NewrelicClientMiddleware {
  def construct[F[_] : Sync, T](t: T): F[T] = Sync[F].delay(t)

  def clientResource[F[_] : Sync](client: Client[F]): Client[F] =
    Client { req: Request[F] =>
      for {
        seg <- Resource.eval(createSegment(req))
        response <- client.run(req)
        newRes <- Resource.eval(completeResponse(req, response, seg))
      } yield newRes
    }

  def createSegment[F[_]:Sync](request: Request[F]): F[Segment] =  construct {
    val txn = AgentBridge.getAgent.getTransaction
    val segment = txn.startSegment("HTTP4S client call")
    segment.addOutboundRequestHeaders(new OutboundRequestWrapper(request))
    segment
  }

  def completeResponse[F[_]: Sync](request: Request[F], response: Response[F], segment: Segment): F[Response[F]] =
    Sync[F].handleErrorWith(construct {
      val txn = AgentBridge.getAgent.getTransaction
      segment.reportAsExternal(HttpParameters
        .library("HTTP4S")
        .uri(new URI(request.uri.toString()))
        .procedure(request.method.toString())
        .inboundHeaders(new InboundResponseWrapper(response))
        .build())
      segment.end()
      response
    })(_ => construct(response))

  def resource[F[_] : ConcurrentEffect](delegate: Resource[F, Client[F]]): Resource[F, Client[F]] =
    delegate.map(clientResource(_))
}

