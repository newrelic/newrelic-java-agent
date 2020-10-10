package com.nr.agent.instrumentation.play28

import com.newrelic.api.agent.Token
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Handler, RequestHeader}

object PlayUtil {

  val newRelicToken: TypedKey[Token] = TypedKey.apply("NR-TOKEN")

  def appendToken(result: (RequestHeader, Handler), token: Token): (RequestHeader, Handler) = {
    (result._1.addAttr(newRelicToken, token), result._2)
  }

}
