package akka.http.scaladsl.server

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}

import scala.concurrent.ExecutionContext

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "akka.http.scaladsl.server.RouteResult")
class RouteResultInstrumentation {
  def route2HandlerFlow(route: Route)(implicit
                                          routingSettings: RoutingSettings,
                                          parserSettings:   ParserSettings,
                                          materializer:     Materializer,
                                          routingLog:       RoutingLog,
                                          executionContext: ExecutionContext = null,
                                          rejectionHandler: RejectionHandler = RejectionHandler.default,
                                          exceptionHandler: ExceptionHandler = null): Flow[HttpRequest, HttpResponse, NotUsed]
      = InstrumentedRouteTransform.routeToFlow(route)
}
