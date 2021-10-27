package akka.http.scaladsl.server

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "akka.http.scaladsl.server.RouteResult")
class RouteResultInstrumentation {
  def routeToFlow(route: Route)(implicit system: ClassicActorSystemProvider):
      Flow[HttpRequest, HttpResponse, NotUsed] = InstrumentedRouteTransform.routeToFlow(route)
}
