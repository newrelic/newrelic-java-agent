/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http

import org.apache.pekko.http.scaladsl.server.Route
import org.junit.rules.ExternalResource

import scala.beans.BeanProperty

class HttpServerRule(@BeanProperty val port: Int, val route: Route)
  extends ExternalResource {
  val server = new HttpServer(route)

  override def before(): Unit = server.start(port)

  override def after(): Unit = server.stop()
}

object HttpServerRule {
  def apply(port: Int, route: Route) = new HttpServerRule(port, route)
}
