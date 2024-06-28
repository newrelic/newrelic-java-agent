package com.newrelic.instrumentable.scala.http4s.ember.server

import cats.effect.Sync
import org.http4s.Request

object RequestProcessor {
  /**
    * Processes an http request.
    * This method is meant to be overwritten by any instrumentation module
    * to add additional instrumentation for http4s-ember-server.
    * @param request
    * @tparam F
    * @return
    */
  def processRequest[F[_]: Sync](request: Request[F]): F[Unit] = Sync[F].delay()

}
