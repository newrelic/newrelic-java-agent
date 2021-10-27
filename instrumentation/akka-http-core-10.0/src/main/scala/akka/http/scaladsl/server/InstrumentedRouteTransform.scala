package akka.http.scaladsl.server

import akka.NotUsed
import akka.http.scaladsl.AsyncRequestHandler
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow

import scala.concurrent.ExecutionContext

object InstrumentedRouteTransform {
  def routeToFlow(route: Route)(implicit
                                routingSettings: RoutingSettings,
                                parserSettings:   ParserSettings,
                                materializer:     Materializer,
                                routingLog:       RoutingLog,
                                executionContext: ExecutionContext = null,
                                rejectionHandler: RejectionHandler = RejectionHandler.default,
                                exceptionHandler: ExceptionHandler = null):
  Flow[HttpRequest, HttpResponse, NotUsed] = {
    val instrumentedRouteFunction = new AsyncRequestHandler(Route.asyncHandler(route))
    Flow[HttpRequest].mapAsync(1)(instrumentedRouteFunction)
  }
}
