package com.nr.instrumentation.http4s

import com.newrelic.api.agent.{ExtendedResponse, HeaderType}
import org.http4s.{Header, Response}
import org.typelevel.ci.CIString


class ResponseWrapper[F[_]](val response: Response[F]) extends ExtendedResponse {

  def getStatus: Int =
    response.status.code

  def getStatusMessage: String =
    response.status.reason

  def getContentType: String =
    response.headers.get(CIString("Content-Type"))
            .map(_.head.value)
            .orNull

  def getHeaderType: HeaderType =
    HeaderType.HTTP

  def setHeader(name: String, value: String): Unit = {
    response.headers.put(Header(name, value))
  }

  def getContentLength: Long =
    response.headers.get(CIString("Content-Length")) match {
      case Some(header) => header.head.value.toLong
      case None => -1L
    }
}

object ResponseWrapper {
  def apply[F[_]](response: Response[F]): ExtendedResponse =
    new ResponseWrapper[F](response)
}

