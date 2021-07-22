package com.nr.instrumentation.http4s

import cats.effect.Sync
import org.http4s.Request

import java.util
import com.newrelic.api.agent.{ExtendedRequest, HeaderType}
import org.http4s.util.CaseInsensitiveString

import collection.JavaConverters._

class RequestWrapper[F[_] : Sync](request: Request[F]) extends ExtendedRequest {

  def getMethod: String = {
    request.method.name
  }

  def getRequestURI: String = {
    request.uri.path.toString()
  }

  def getRemoteUser: String = {
    null
  }

  def getParameterNames: java.util.Enumeration[_] = {
    request.uri.query.params.keysIterator.asJavaEnumeration
  }

  def getParameterValues(name: String): Array[String] = {
    request.uri.query.multiParams.getOrElse(name, Seq.empty).toArray
  }

  def getAttribute(name: String): AnyRef = {
    null
  }

  def getCookieValue(name: String): String =
    request.cookies
           .find(_.name.equalsIgnoreCase(name))
           .map(_.content)
           .orNull

  def getHeaderType: HeaderType = {
    HeaderType.HTTP
  }

  def getHeader(name: String): String =
    request.headers
           .find(_.name == CaseInsensitiveString(name))
           .map(_.value)
           .orNull

  override def getHeaders(name: String): util.List[String] =
    request.headers
           .filter(_.name == CaseInsensitiveString(name))
           .toList
           .map(_.value)
           .asJava
}

object RequestWrapper {
  def apply[F[_] : Sync](request: Request[F]): ExtendedRequest =
    new RequestWrapper[F](request)
}
