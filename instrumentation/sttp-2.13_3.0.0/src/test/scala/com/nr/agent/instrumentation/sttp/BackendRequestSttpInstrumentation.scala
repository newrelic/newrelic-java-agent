/*
*
*  * Copyright 2021 New Relic Corporation. All rights reserved.
*  * SPDX-License-Identifier: Apache-2.0
*
*/

package com.nr.agent.instrumentation.sttp

import com.newrelic.agent.introspec.internal.HttpServerRule
import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.nr.agent.instrumentation.sttp.SttpTestUtils.{getSegments, getTraces, makeRequest}
import org.junit.runner.RunWith
import org.junit.{Assert, Rule, Test}
import sttp.client3.{HttpURLConnectionBackend, _}

import java.util.concurrent.TimeUnit

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("sttp"))
class BackendRequestSttpInstrumentation {

  val _server = new HttpServerRule()

  @Rule
  implicit def server: HttpServerRule = _server

  @Test
  def httpURLConnectionBackend(): Unit = {
    //Given
    implicit val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

    //When
    val response = makeRequest

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
