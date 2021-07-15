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
import com.nr.agent.instrumentation.sttp.DelegateConcurrentEffect
import fs2.Stream
import org.http4s.{Request => Http4sRequest}
import org.http4s.client.Client
import sttp.client.http4s.Http4sBackend.EncodingHandler

@ScalaWeave(`type` = ScalaMatchType.Object, `originalName` = "sttp.client.http4s.Http4sBackend")
class Http4sBackend_Instrumentation {
  def usingClient[F[_]: ConcurrentEffect: ContextShift](
                                                         client: Client[F],
                                                         blocker: Blocker,
                                                         customizeRequest: Http4sRequest[F] => Http4sRequest[F],
                                                         customEncodingHandler: EncodingHandler[F]
                                                       ): SttpBackend[F, Stream[F, Byte], NothingT] = new DelegateConcurrentEffect[F](Weaver.callOriginal())
}

