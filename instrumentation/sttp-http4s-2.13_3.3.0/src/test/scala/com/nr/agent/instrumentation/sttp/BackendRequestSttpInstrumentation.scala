/*
*
*  * Copyright 2021 New Relic Corporation. All rights reserved.
*  * SPDX-License-Identifier: Apache-2.0
*
*/

package com.nr.agent.instrumentation.sttp

import cats.effect.{Blocker, ContextShift, IO}
import com.newrelic.agent.introspec.internal.HttpServerRule
import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.nr.agent.instrumentation.sttp.SttpTestUtils.{getSegments, getTraces, makeRequest}
import org.http4s.client.blaze.BlazeClientBuilder
import org.junit.{Assert, Rule, Test}
import org.junit.runner.RunWith
import sttp.client3._
import sttp.client3.http4s.Http4sBackend

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("sttp"))
class BackendRequestSttpInstrumentation {

  val _server = new HttpServerRule()

  @Rule
  implicit def server: HttpServerRule = _server

  @Test
  def http4sBackend(): Unit = {
    //Given
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val response = Blocker[IO].use(blocker => {
      BlazeClientBuilder[IO](blocker.blockingContext).resource.use(client => {
        implicit val backend: SttpBackend[IO, Any] = Http4sBackend.usingClient(client, blocker)
        makeRequest
      })
    }).unsafeRunSync()

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertTrue("Successful response", response.code.isSuccess)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 2, segments.size)
  }
}
