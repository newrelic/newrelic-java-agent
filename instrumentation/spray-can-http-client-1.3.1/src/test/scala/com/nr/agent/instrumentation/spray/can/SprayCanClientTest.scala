/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spray.can

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.agent.introspec.HttpTestServer
import com.newrelic.agent.introspec.CatHelper
import com.newrelic.agent.introspec.ExternalRequest
import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector
import com.newrelic.agent.introspec.MetricsHelper
import com.newrelic.agent.introspec.TransactionEvent
import com.newrelic.agent.introspec.internal.HttpServerLocator
import com.newrelic.api.agent.Trace

import java.net.URI
import java.util.Collection
import org.junit.{After, Assert}
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import spray.can.Http.HostConnectorInfo

import scala.concurrent._
import scala.concurrent.duration._
import akka.io.IO
import akka.util.Timeout
import akka.pattern.ask
import akka.actor._
import spray.can.Http
import spray.http._
import HttpMethods._
import com.newrelic.test.marker.Java17IncompatibleTest
import org.junit.experimental.categories.Category


@Category(Array(classOf[Java17IncompatibleTest]))
@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("spray.", "akka.", "scala.", "com.nr.agent.instrumentation."))
class SprayCanClientTest extends ConnectionLevelApiDemo {
  private implicit val timeout: Timeout = 5.seconds
  implicit val system = akka.actor.ActorSystem()

  import system.dispatcher // execution context

  val externalServer: HttpTestServer = SprayCanClientTest.EXTERNAL_SERVER;
  val endpoint: URI = externalServer.getEndPoint;
  val host: String = endpoint.getHost
  val port: Integer = endpoint.getPort

  @Before
  def before() = {
    System.out.println("==============================================")
  }

  @After
  def after() = {
    System.out.println("==============================================")
  }

  private def assertTransactionCount(introspector: Introspector, count: Int) {
    if (count != introspector.getFinishedTransactionCount(2000)) {
      var failureMsg = "Expected transaction count of " + count + " but found count of " + introspector.getFinishedTransactionCount() + ". Reported Transactions:"
      val it: java.util.Iterator[String] = introspector.getTransactionNames().iterator()
      while (it.hasNext()) {
        val txName: String = it.next();
        failureMsg += "\n" + txName
      }
      Assert.fail(failureMsg);
    }
  }

  /**
    * Return the first transaction name that matches the regex .*ClientTest.*
    */
  private def getCanClientTx(introspector: Introspector): String = {
    var clientTx: String = null;
    val it: java.util.Iterator[String] = introspector.getTransactionNames().iterator()
    while (it.hasNext()) {
      val txName: String = it.next();
      if (txName.matches(".*ClientTest.*")) {
        clientTx = txName;
      }
    }
    Assert.assertNotNull("No .*ClientTest.* tx found.", clientTx)
    clientTx
  }

  ///////////////// General asserts. All three uses of the api hit the same endpoint so they should generate the same metrics.

  // one external call without cat
  def assertExternalMetrics(): Unit = {
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector()
    assertTransactionCount(introspector, 6);
    val clientTx: String = getCanClientTx(introspector)

    // external rollups
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/all"));
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/allOther"));
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/" + host + "/SprayCanClient/connection"));

    // Events
    val transactionEvents: Collection[TransactionEvent] = introspector.getTransactionEvents(clientTx);
    Assert.assertEquals(3, transactionEvents.size());
    var transactionEvent: TransactionEvent = transactionEvents.iterator().next();
    Assert.assertEquals(1, transactionEvent.getExternalCallCount());
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);
    transactionEvent = transactionEvents.iterator().next();
    Assert.assertEquals(1, transactionEvent.getExternalCallCount());
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);
    transactionEvent = transactionEvents.iterator().next();
    Assert.assertEquals(1, transactionEvent.getExternalCallCount());
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);

    val scopedMetricName: String = "External/" + host + "/SprayCanClient/connection";
    Assert.assertEquals(3, MetricsHelper.getScopedMetricCount(clientTx, scopedMetricName));
    Assert.assertTrue("Expected external segment of one second or greater for scoped metric: " + scopedMetricName,
      MetricsHelper.getScopedMetricExclusiveTimeInSec(clientTx, scopedMetricName) >= 1f);

    val externalRequests: Collection[ExternalRequest] = introspector.getExternalRequests(clientTx);
    Assert.assertEquals(1, externalRequests.size());
    val externalRequest: ExternalRequest = externalRequests.iterator().next();
    Assert.assertEquals(3, externalRequest.getCount());
    Assert.assertEquals(host, externalRequest.getHostname());
    Assert.assertEquals("SprayCanClient", externalRequest.getLibrary());
    Assert.assertEquals("connection", externalRequest.getOperation());
  }

  // one external call with cat
  def assertCatMetrics(): Unit = {
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    assertTransactionCount(introspector, 6)
    val clientTx: String = getCanClientTx(introspector)

    // external rollups
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/all"))
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/allOther"))
    Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"))

    // Events
    val transactionEvents: Collection[TransactionEvent] = introspector.getTransactionEvents(clientTx)
    Assert.assertEquals(3, transactionEvents.size())
    var transactionEvent: TransactionEvent = transactionEvents.iterator().next()
    Assert.assertEquals(1, transactionEvent.getExternalCallCount)
    Assert.assertTrue(transactionEvent.getExternalDurationInSec > 0)
    transactionEvent = transactionEvents.iterator().next()
    Assert.assertEquals(1, transactionEvent.getExternalCallCount())
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0)
    transactionEvent = transactionEvents.iterator().next()
    Assert.assertEquals(1, transactionEvent.getExternalCallCount())
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0)

    val scopedMetricName: String = "ExternalTransaction/" + host + "/" + externalServer.getCrossProcessId + "/" + externalServer.getServerTransactionName
    Assert.assertEquals(3, MetricsHelper.getScopedMetricCount(clientTx, scopedMetricName));
    Assert.assertTrue("Expected external segment of one second or greater for scoped metric: " + scopedMetricName,
      MetricsHelper.getScopedMetricExclusiveTimeInSec(clientTx, scopedMetricName) >= 1f);

    val externalRequests: Collection[ExternalRequest] = introspector.getExternalRequests(clientTx);
    Assert.assertEquals(1, externalRequests.size());
    val externalRequest: ExternalRequest = externalRequests.iterator().next();
    Assert.assertEquals(3, externalRequest.getCount());
    Assert.assertEquals(host, externalRequest.getHostname());

    CatHelper.verifyMultipleSuccessfulCat(introspector, clientTx, 3);
  }

  /////////////////////////// Connection api

  @Test
  def testConnectionExternal() {
    InstrumentationTestRunner.getIntrospector.clear()
    // Create multiple requests because spray-can-client re-uses certain objects for requests going
    // to the same endpoint and without calling this multiple times the test may report a false positive.
    connectionApiTransaction(false)
    connectionApiTransaction(false)
    connectionApiTransaction(false)
    assertExternalMetrics()
  }

  @Test
  def testConnectionCat() {
    InstrumentationTestRunner.getIntrospector.clear()
    connectionApiTransaction(true)
    connectionApiTransaction(true)
    connectionApiTransaction(true)
    assertCatMetrics()
  }

  @Trace(dispatcher = true, nameTransaction = true)
  def connectionApiTransaction(doCat: Boolean) {
    val resultFuture: Future[HttpResponse] = demoConnectionLevelApi(host, port, doCat)
    Await.result(resultFuture, 30 seconds)
  }

  /////////////////////////// Host api

  @Test
  def testHostExternal() {
    hostApiTransaction(false)
    hostApiTransaction(false)
    hostApiTransaction(false)
    assertExternalMetrics()
  }

  @Test
  def testHostCat() {
    hostApiTransaction(true)
    hostApiTransaction(true)
    hostApiTransaction(true)
    assertCatMetrics()
  }

  @Trace(dispatcher = true, nameTransaction = true)
  def hostApiTransaction(doCat: Boolean) {
    val resultFuture: Future[HttpResponse] = demoHostLevelApi(host, doCat)
    Await.result(resultFuture, 30 seconds)
  }

  def demoHostLevelApi(host: String, doCat: Boolean)(implicit system: ActorSystem): Future[HttpResponse] = {
    for {
      Http.HostConnectorInfo(hostConnector, _) <- IO(Http) ? Http.HostConnectorSetup(host, port = externalServer.getEndPoint.getPort)
      response <- hostConnector.ask(HttpRequest(GET, "/", headers = List(HttpHeaders.RawHeader(HttpTestServer.SLEEP_MS_HEADER_KEY, "1000"), HttpHeaders.RawHeader(HttpTestServer.DO_CAT, doCat.toString)))).mapTo[HttpResponse]
    } yield {
      system.log.info("Host-Level API: received {} response with {} bytes",
        response.status, response.entity.data.length)
      response
    }
  }

  /////////////////////////// Request api

  @Test
  def testRequestExternal() {
    requestApiTransaction(false)
    requestApiTransaction(false)
    requestApiTransaction(false)
    assertExternalMetrics()
  }

  @Test
  def testRequestCat() {
    requestApiTransaction(true)
    requestApiTransaction(true)
    requestApiTransaction(true)
    assertCatMetrics()
  }

  @Trace(dispatcher = true, nameTransaction = true)
  def requestApiTransaction(doCat: Boolean) {
    val resultFuture: Future[HttpResponse] = demoRequestLevelApi(host, doCat)
    Await.result(resultFuture, 30 seconds)
  }

  def demoRequestLevelApi(host: String, doCat: Boolean)(implicit system: ActorSystem): Future[HttpResponse] = for {
    response <- IO(Http).ask(HttpRequest(GET, Uri(s"http://$host:$port/"), headers = List(HttpHeaders.RawHeader(HttpTestServer.SLEEP_MS_HEADER_KEY, "1000"), HttpHeaders.RawHeader(HttpTestServer.DO_CAT, doCat.toString)))).mapTo[HttpResponse]
  } yield {
    system.log.info("Request-Level API: received {} response with {} bytes",
      response.status, response.entity.data.length)
    response
  }


}

object SprayCanClientTest {
  val EXTERNAL_SERVER: HttpTestServer = HttpServerLocator.createAndStart()

  @AfterClass
  def afterClass() {
    EXTERNAL_SERVER.shutdown()
  }
}

trait ConnectionLevelApiDemo {
  private implicit val timeout: Timeout = 5.seconds
  private implicit val nrContext: ConnectionLevelApiDemo = this

  def demoConnectionLevelApi(host: String, port: Integer, doCat: Boolean)(implicit system: ActorSystem): Future[HttpResponse] = {
    val actor = system.actorOf(Props(new MyRequestActor(host, port)))
    AgentBridge.getAgent.getTransaction.registerAsyncActivity(nrContext)
    val future = actor ? HttpRequest(GET, "/", headers = List(HttpHeaders.RawHeader(HttpTestServer.SLEEP_MS_HEADER_KEY, "1000"), HttpHeaders.RawHeader(HttpTestServer.DO_CAT, doCat.toString)))
    future.mapTo[HttpResponse]
  }

  // The connection-level API is the lowest-level way to access the spray-can client-side infrastructure.
  // With it you are in charge of establishing, using, and tearing down the HTTP connections yourself.
  // The benefit is that you have complete control over when connections are being established and torn down
  // as well as how requests are scheduled onto them.

  // Actor that manages the lifecycle of a single HTTP connection for a single request
  class MyRequestActor(host: String, port: Integer)(implicit nrContext: ConnectionLevelApiDemo) extends Actor with ActorLogging {

    import context.system

    override def receive: Receive = {
      case request: HttpRequest =>
        initConnection(request)
    }

    @Trace(async = true)
    def initConnection(request: HttpRequest): Unit = {
      AgentBridge.getAgent.startAsyncActivity(nrContext)
      System.out.println(Thread.currentThread().getId + ": Joined transaction? : " + AgentBridge.getAgent.getTransaction(false))
      log.info("Connecting to host {}", host)
      // start by establishing a new HTTP connection
      Assert.assertNotNull(AgentBridge.getAgent.getTransaction(false))
      IO(Http) ! Http.Connect(host, port)
      context.become(connecting(sender(), request))
    }

    def connecting(commander: ActorRef, request: HttpRequest): Receive = {
      case _: Http.Connected =>
        // once connected, we can send the request across the connection
        log.info("Sending request to: {} on thread: {}", sender(), Thread.currentThread.getId)
        sender() ! request
        context.become(waitingForResponse(commander))

      case Http.CommandFailed(Http.Connect(address, _, _, _, _)) =>
        log.warning("Could not connect to {}", address)
        commander ! Status.Failure(new RuntimeException("Connection error. Could not connect to " + address))
        context.stop(self)
    }

    def waitingForResponse(commander: ActorRef): Receive = {
      case response@HttpResponse(status, entity, _, _) =>
        log.info("Connection-Level API: received {} response with {} bytes", status, entity.data.length)
        log.info("Got response. Sending close to: {}", sender())
        sender() ! Http.Close
        context.become(waitingForClose(commander, response))

      case ev@(Http.SendFailed(_) | Timedout(_)) =>
        log.warning("Received {}", ev)
        commander ! Status.Failure(new RuntimeException("Request error"))
        context.stop(self)
    }

    def waitingForClose(commander: ActorRef, response: HttpResponse): Receive = {
      case ev: Http.ConnectionClosed =>
        log.info("Connection closed ({})", ev)
        commander ! Status.Success(response)
        context.stop(self)

      case Http.CommandFailed(Http.Close) =>
        log.warning("Could not close connection")
        commander ! Status.Failure(new RuntimeException("Connection close error"))
        context.stop(self)
    }
  }

}
