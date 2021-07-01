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
import com.nr.agent.instrumentation.sttp.Sttp2TestUtils.{getSegments, getTraces, makeRequest}
import org.junit.{Assert, Rule, Test}
import org.junit.runner.RunWith
import sttp.client.{HttpURLConnectionBackend, _}
import sttp.client.akkahttp.AkkaHttpBackend
import sttp.client.http4s.Http4sBackend

import scala.concurrent.duration.DurationInt
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, ExecutionContext, Future}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("none"))
class BackendRequestNoInstrumentation {

  val _server = new HttpServerRule()

  @Rule
  implicit def server: HttpServerRule = _server

  @Test
  def httpURLConnectionBackend(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    //When
    val response = makeRequest

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertTrue("Successful response", response.code.isSuccess)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 1, segments.size)
  }

  @Test
  def akkaHttpBackend(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val backend: SttpBackend[Future, Nothing, NothingT] = AkkaHttpBackend()

    //When
    val response = Await.result(makeRequest, 10.seconds)

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertTrue("Successful response", response.code.isSuccess)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 1, segments.size)
  }

  @Test
  def http4sBackend(): Unit = {
    //Given
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector

    //When
    val response = Blocker[IO].flatMap(Http4sBackend.usingDefaultClientBuilder[IO](_)).use { implicit backend => makeRequest }.unsafeRunSync()

    //Then
    introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10))

    val traces = getTraces()
    val segments = getSegments(traces)

    Assert.assertTrue("Successful response", response.code.isSuccess)
    Assert.assertEquals("Transactions", 1, introspector.getTransactionNames.size)
    Assert.assertEquals("Traces", 1, traces.size)
    Assert.assertEquals("Segments", 1, segments.size)
  }
}
