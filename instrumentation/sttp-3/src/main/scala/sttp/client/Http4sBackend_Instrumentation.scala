/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package sttp.client

import cats.effect.{Blocker, ConcurrentEffect, ContextShift}
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}
import com.nr.agent.instrumentation.sttp.DelegateAsync
import org.http4s.Request
import org.http4s.client.Client
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.http4s.Http4sBackend.EncodingHandler

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "sttp.client3.http4s.Http4sBackend")
class Http4sBackend_Instrumentation {
  def usingClient[F[_] : ConcurrentEffect : ContextShift](
                                                           client: Client[F],
                                                           blocker: Blocker,
                                                           customizeRequest: Request[F] => Request[F],
                                                           customEncodingHandler: EncodingHandler[F]
                                                         ): SttpBackend[F, Fs2Streams[F]] = new DelegateAsync[F, Fs2Streams[F]](Weaver.callOriginal())
}
