/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play26

import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class SimpleScalaController extends Controller {

  def scalaHello = Action.async {
    Future {
      Ok("Scala says hello world")
    }
  }
}
