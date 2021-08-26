package com.nr.instrumentation.http4s

import com.newrelic.api.agent.{HeaderType, OutboundHeaders}
import org.http4s.Request

class OutboundRequestWrapper[F[_]](val request: Request[F]) extends OutboundHeaders {
  /**
    * Return the type of header key syntax used for this.
    *
    * @return An <code>enum</code> specifying the type of headers present.
    * @since 3.5.0
    */
  override def getHeaderType: HeaderType = HeaderType.HTTP

  /**
    * Sets a response header with the given name and value.
    * NO-OP HTTP4s Request Headers are immutable and so can't be set from here
    */
  override def setHeader(name: String, value: String): Unit = {
  }
}
