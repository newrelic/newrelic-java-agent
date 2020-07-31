/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play26

import com.newrelic.api.agent.Token
import play.api.Application
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Handler, RequestHeader, Result}

import scala.concurrent.Future

object PlayUtil {

  val newRelicToken: TypedKey[Token] = TypedKey.apply("NR-TOKEN")

  def appendToken(result: Either[Future[Result], (RequestHeader, Handler, Application)], token: Token) : Either[Future[Result], (RequestHeader, Handler, Application)] = {
    var tempResult = result
    if (tempResult.isRight) {
      tempResult = result.right.flatMap(values => {
        val newRequestHeader = values._1.addAttr(newRelicToken, token)
        Right(newRequestHeader, values._2, values._3)
      })
    }
    tempResult
  }
  
}
