/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play27

import com.newrelic.api.agent.Token
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Handler, RequestHeader}

object PlayUtil {

  val newRelicToken: TypedKey[Token] = TypedKey.apply("NR-TOKEN")

  def appendToken(result: (RequestHeader, Handler), token: Token) : (RequestHeader, Handler) = {
    (result._1.addAttr(newRelicToken, token), result._2)
  }

}
