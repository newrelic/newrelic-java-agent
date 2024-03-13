package com.nr.instrumentation.http4s

import com.newrelic.api.agent.{ExtendedInboundHeaders, HeaderType}
import org.http4s.Headers
import org.typelevel.ci.CIString

import scala.jdk.CollectionConverters._
import java.util

class InboundResponseWrapper[F[_]](headers: Headers) extends ExtendedInboundHeaders {
  /**
    * Return the type of header key syntax used for this.
    *
    * @return An <code>enum</code> specifying the type of headers present.
    * @since 3.5.0
    */
  override def getHeaderType: HeaderType = HeaderType.HTTP

  /**
    * Returns the value of the specified request header as a <code>String</code>. If the request does not include a
    * header with the specified input name, then this method returns <code>null</code>.
    *
    * @param name The name of the desired request header.
    * @return A <code>String</code> containing the value of the specified input request header, or <code>null</code> if the request header is not present.
    * @since 3.5.0
    */
  override def getHeader(name: String): String =
    headers.headers.find(_.name == CIString(name)).map(_.value).orNull

  override def getHeaders(name: String): util.List[String] =
    headers.headers.map(_.name.toString).asJava
}
