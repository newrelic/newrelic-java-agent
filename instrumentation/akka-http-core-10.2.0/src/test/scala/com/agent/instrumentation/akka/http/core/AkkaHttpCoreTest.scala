/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.akka.http.core

import java.util
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
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
@InstrumentationTestConfig(includePrefixes = Array("scala", "akka"))
class AkkaHttpCoreTest {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val akkaServer = new AkkaServer()
  val playServer = new PlayServer()
  var port: Int = InstrumentationTestRunner.getIntrospector.getRandomPort
  val baseUrl: String = "http://localhost:" + port

  @Test
  def syncHandlerAkkaServerTest(): Unit = {
    akkaServer.start(port, async = false)

    Http().singleRequest(HttpRequest(uri = baseUrl + "/ping"))

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    akkaServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
  }

  @Test
  def asyncHandlerAkkaServerTest(): Unit = {
    akkaServer.start(port, async = true)
    Http().singleRequest(HttpRequest(uri = baseUrl + "/asyncPing"))

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 1)
    akkaServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
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
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
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
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
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
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
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
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
  }

  @Test
  def syncHandlerAkkaServerCatTest(): Unit = {
    akkaServer.start(port, async = false)

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
    akkaServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
  }

  @Test
  def asyncHandlerAkkaServerCatTest(): Unit = {
    akkaServer.start(port, async = true)

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
    akkaServer.stop()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val txName = introspector.getTransactionNames.iterator.next
    Assert.assertEquals("WebTransaction/AkkaHttpCore/akkaHandler", txName)
  }

  @Test
  def asyncHandlerAkkaServerHttpClientTest(): Unit = {
    akkaServer.start(port, async = true)
    makeHttpRequest(true)

    verifyHttpClientMetrics()
  }

  @Test
  def syncHandlerAkkaServerHttpClientTest(): Unit = {
    akkaServer.start(port, async = false)
    makeHttpRequest(false)

    verifyHttpClientMetrics()
  }

  def verifyHttpClientMetrics() {
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    awaitFinishedTx(introspector, 2)
    akkaServer.stop()
    Assert.assertEquals(2, introspector.getFinishedTransactionCount())
    val txNames = introspector.getTransactionNames

    val clientTx = "OtherTransaction/Custom/com.agent.instrumentation.akka.http.core.AkkaHttpCoreTest/makeHttpRequest"
    val serverTx = "WebTransaction/AkkaHttpCore/akkaHandler"

    // The @Trace(dispatcher = true) below where the Http call is made
    Assert.assertTrue(txNames.contains(clientTx))
    // The WebTransaction from the AkkaHttp server itself
    Assert.assertTrue(txNames.contains(serverTx))

    Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(clientTx, "External/localhost/AkkaHttpClient/GET"))
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/AkkaHttpClient/GET"))

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
    Assert.assertEquals("AkkaHttpClient", externalRequest.getLibrary)
    Assert.assertEquals("GET", externalRequest.getOperation)
  }

  @Trace(dispatcher = true, nameTransaction = true)
  private def makeHttpRequest(async: Boolean): Unit = {
    Http().singleRequest(HttpRequest(uri = baseUrl + (if (async) "/asyncPing" else "/ping")))
  }

  private def awaitFinishedTx(introspector: Introspector, expectedTxCount: Int) {
    val timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30) // 30 seconds from now
    while (System.currentTimeMillis() < timeout && introspector.getFinishedTransactionCount() <= expectedTxCount - 1) {
      Thread.sleep(100)
    }
    Thread.sleep(100)
  }
}
