/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http.core

import java.util
import java.util.concurrent.TimeUnit
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import com.newrelic.agent.HeadersUtil
import com.newrelic.agent.introspec._
import com.newrelic.agent.util.Obfuscator
import com.newrelic.api.agent.Trace
import org.junit.Assert.{assertNotNull, assertTrue}
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala", "org.apache.pekko"))
class PekkoHttpCoreTest {

  implicit val system: ActorSystem = ActorSystem()

  val pekkoServer = new PekkoServer()
  val playServer = new PlayServer()
  var port: Int = InstrumentationTestRunner.getIntrospector.getRandomPort
  val baseUrl: String = "http://localhost:" + port

  @Test
  def syncHandlerPekkoServerTest(): Unit = {
    pekkoServer.start(port, async = false)

    val response: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = baseUrl + "/ping"))

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    pekkoServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def asyncHandlerPekkoServerTest(): Unit = {
    pekkoServer.start(port, async = true)
    Http().singleRequest(HttpRequest(uri = baseUrl + "/asyncPing"))

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    pekkoServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def syncHandlerPlayServerTest(): Unit = {
    playServer.start(port, async = false)
    Http().singleRequest(HttpRequest(uri = baseUrl + "/ping"))

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    playServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def asyncHandlerPlayServerTest(): Unit = {
    playServer.start(port, async = true)

    Http().singleRequest(HttpRequest(uri = baseUrl + "/asyncPing"))

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    playServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def syncHandlerPlayServerCatTest(): Unit = {
    playServer.start(port, async = false)

    val idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead")

    val requestHeaders: Seq[HttpHeader] = List(new RawHeader(HeadersUtil.NEWRELIC_ID_HEADER, idHeader))
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = baseUrl + "/ping", headers = collection.immutable.Seq[HttpHeader](requestHeaders: _*)))

    val result: HttpResponse = Await.result(responseFuture, new DurationInt(10).seconds)
    var hasNewRelicHeader: Boolean = false
    hasNewRelicHeader = result.headers.exists(header => header.name().contains(HeadersUtil.NEWRELIC_APP_DATA_HEADER))

    Assert.assertTrue(hasNewRelicHeader)

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    playServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def asyncHandlerPlayServerCatTest(): Unit = {
    playServer.start(port, async = true)

    val idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead")
    val requestHeaders: Seq[HttpHeader] = List(new RawHeader(HeadersUtil.NEWRELIC_ID_HEADER, idHeader))

    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = baseUrl + "/asyncPing", headers = collection.immutable.Seq[HttpHeader](requestHeaders: _*)))

    val result: HttpResponse = Await.result(responseFuture, new DurationInt(1).seconds)
    var hasNewRelicHeader: Boolean = false
    hasNewRelicHeader = result.headers.exists(header => header.name().contains(HeadersUtil.NEWRELIC_APP_DATA_HEADER))
    Assert.assertTrue(hasNewRelicHeader)

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    playServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def syncHandlerPekkoServerCatTest(): Unit = {
    pekkoServer.start(port, async = false)

    val idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead")
    val requestHeaders: Seq[HttpHeader] = List(new RawHeader(HeadersUtil.NEWRELIC_ID_HEADER, idHeader))

    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = baseUrl + "/ping", headers = collection.immutable.Seq[HttpHeader](requestHeaders: _*)))

    val result: HttpResponse = Await.result(responseFuture, new DurationInt(10).seconds)
    var hasNewRelicHeader: Boolean = false
    hasNewRelicHeader = result.headers.exists(header => header.name().contains(HeadersUtil.NEWRELIC_APP_DATA_HEADER))
    Assert.assertTrue(hasNewRelicHeader)

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    pekkoServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def asyncHandlerPekkoServerCatTest(): Unit = {
    pekkoServer.start(port, async = true)

    val idHeader = Obfuscator.obfuscateNameUsingKey("1xyz234#1xyz3333", "cafebabedeadbeef8675309babecafe1beefdead")
    val requestHeaders: Seq[HttpHeader] = List(new RawHeader(HeadersUtil.NEWRELIC_ID_HEADER, idHeader))

    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = baseUrl + "/asyncPing", headers = collection.immutable.Seq[HttpHeader](requestHeaders: _*)))

    val result: HttpResponse = Await.result(responseFuture, new DurationInt(1).seconds)
    var hasNewRelicHeader: Boolean = false
    hasNewRelicHeader = result.headers.exists(header => header.name().contains(HeadersUtil.NEWRELIC_APP_DATA_HEADER))
    Assert.assertTrue(hasNewRelicHeader)

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    pekkoServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/PekkoHttpCore/pekkoHandler", txName)
  }

  @Test
  def asyncHandlerPekkoServerHttpClientTest(): Unit = {
    pekkoServer.start(port, async = true)
    makeHttpRequest(true)

    verifyHttpClientMetrics()
  }

  @Test
  def syncHandlerPekkoServerHttpClientTest(): Unit = {
    pekkoServer.start(port, async = false)
    makeHttpRequest(false)

    verifyHttpClientMetrics()
  }

  def verifyHttpClientMetrics() = {
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 2)
    pekkoServer.stop()
    Assert.assertEquals(2, introspector.getFinishedTransactionCount())
    val txNames = introspector.getTransactionNames

    val clientTx = "OtherTransaction/Custom/com.agent.instrumentation.org.apache.pekko.http.core.PekkoHttpCoreTest/makeHttpRequest"
    val serverTx = "WebTransaction/PekkoHttpCore/pekkoHandler"

    // The @Trace(dispatcher = true) below where the Http call is made
    Assert.assertTrue(txNames.contains(clientTx))
    // The WebTransaction from the PekkoHttp server itself
    Assert.assertTrue(txNames.contains(serverTx))

    Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(clientTx, "External/localhost/PekkoHttpClient/GET"))
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/PekkoHttpClient/GET"))

    // external rollups
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/all"))
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"))
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"))

    // verify timing of External/all metrics
    val externalMetrics: TracedMetricData = InstrumentationTestRunner.getIntrospector.getUnscopedMetrics.get("External/all")
    assertNotNull(externalMetrics)
    assertTrue(externalMetrics.getTotalTimeInSec > 0)

    val transactionEvents: util.Collection[TransactionEvent] = introspector.getTransactionEvents(clientTx)
    Assert.assertEquals(1, transactionEvents.size)
    val transactionEvent: TransactionEvent = transactionEvents.iterator.next
    Assert.assertEquals(1, transactionEvent.getExternalCallCount)
    Assert.assertTrue(transactionEvent.getExternalDurationInSec > 0)

    val externalRequests: util.Collection[ExternalRequest] = introspector.getExternalRequests(clientTx)
    Assert.assertEquals(1, externalRequests.size)
    val externalRequest: ExternalRequest = externalRequests.iterator.next
    Assert.assertEquals(1, externalRequest.getCount)
    Assert.assertEquals("localhost", externalRequest.getHostname)
    Assert.assertEquals("PekkoHttpClient", externalRequest.getLibrary)
    Assert.assertEquals("GET", externalRequest.getOperation)
  }

  @Trace(dispatcher = true, nameTransaction = true)
  private def makeHttpRequest(async: Boolean): Unit = {
    Http().singleRequest(HttpRequest(uri = baseUrl + (if (async) "/asyncPing" else "/ping")))
  }

  private def awaitFinishedTx(introspector: Introspector, expectedTxCount: Int) = {
    val timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30) // 30 seconds from now
    while (System.currentTimeMillis() < timeout && introspector.getFinishedTransactionCount() <= expectedTxCount - 1) {
      Thread.sleep(100)
    }
    Thread.sleep(100)
  }
}
