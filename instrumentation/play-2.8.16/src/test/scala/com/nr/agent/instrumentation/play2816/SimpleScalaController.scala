/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play2816

import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SimpleScalaController @Inject() (components: ControllerComponents) extends AbstractController(components) {

  def scalaHello = Action.async {
    Future {
      Ok("Scala says hello world")
    }
  }

}
