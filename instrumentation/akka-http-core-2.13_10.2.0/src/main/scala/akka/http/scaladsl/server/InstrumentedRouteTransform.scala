package akka.http.scaladsl.server

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.AsyncRequestHandler
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow

import scala.concurrent.ExecutionContext

object InstrumentedRouteTransform {
  def routeToFlow(route: Route)(implicit system: ClassicActorSystemProvider):
  Flow[HttpRequest, HttpResponse, NotUsed] = {
    implicit val ec: ExecutionContext = system.classicSystem.dispatcher
    val instrumentedRouteFunction = new AsyncRequestHandler(Route.toFunction(route))
    Flow[HttpRequest].mapAsync(1)(instrumentedRouteFunction)
  }
}
