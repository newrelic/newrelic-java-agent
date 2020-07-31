/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.spray

import akka.actor.ActorLogging
import spray.routing.Directives._
import spray.routing.{HttpServiceActor, RequestContext, Route}
import spray.routing.directives.PathDirectives

class RouteService(val route: Route) extends HttpServiceActor with ActorLogging {
  def receive = runRoute {
    route
  }
}

object RouteService {
  val defaultRoute = {
    path("test") {
      get { (ctx: RequestContext) =>
        ctx.complete("FAIL")
      }
    }
  }
}