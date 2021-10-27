package akka.http.scaladsl.server

import scala.concurrent.ExecutionContext
import akka.NotUsed
import akka.http.scaladsl.settings.{ RoutingSettings, ParserSettings }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }


import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}


@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "akka.http.scaladsl.server.RouteResult")
class RouteResultInstrumentation {
  def route2HandlerFlow(route: Route)(
    implicit
    routingSettings:  RoutingSettings,
    parserSettings:   ParserSettings,
    materializer:     Materializer,
    routingLog:       RoutingLog,
    executionContext: ExecutionContext = null,
    rejectionHandler: RejectionHandler = RejectionHandler.default,
    exceptionHandler: ExceptionHandler = null
  ): Flow[HttpRequest, HttpResponse, NotUsed] = InstrumentedRouteTransform.routeToFlow(route)
}
