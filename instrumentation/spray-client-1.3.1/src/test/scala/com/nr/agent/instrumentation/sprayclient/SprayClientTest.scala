/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sprayclient

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
import com.newrelic.test.marker.Java17IncompatibleTest

import java.net.URI
import java.util.Collection
import org.junit.Assert
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import spray.http._
import spray.client.pipelining._

import scala.concurrent._
import scala.concurrent.duration._

@Category(Array(classOf[Java17IncompatibleTest]))
@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("spray."))
class SprayClientTest {
  implicit val system = akka.actor.ActorSystem()
  import system.dispatcher // execution context

  @Test
  def testSimple() {
    val server :HttpTestServer = HttpServerLocator.createAndStart();
    val endpoint :URI = server.getEndPoint();
    val host :String  = endpoint.getHost();
    simpleTransaction(endpoint.toString())
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 2);
    Assert.assertEquals(2, introspector.getFinishedTransactionCount())

    var txOne :String = null;
    val it :java.util.Iterator[String] = introspector.getTransactionNames().iterator()
    while(it.hasNext()) {
      val txName :String = it.next();
      if (txName.matches(".*SprayClientTest.*")) {
        txOne = txName;
      }
    }
    Assert.assertNotNull("Unable to find transaction", txOne);

    // external rollups
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/SprayClient/sendReceive"));

    // Events
    val transactionEvents :Collection[TransactionEvent] = introspector.getTransactionEvents(txOne);
    Assert.assertEquals(1, transactionEvents.size());
    val transactionEvent :TransactionEvent = transactionEvents.iterator().next();
    Assert.assertEquals(1, transactionEvent.getExternalCallCount());
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);
    
    val scopedMetricName :String = "External/" + host + "/SprayClient/sendReceive";
    Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, scopedMetricName));
    Assert.assertTrue("Expected external segment of one second or greater for scoped metric: "+ scopedMetricName,
    MetricsHelper.getScopedMetricExclusiveTimeInSec(txOne, scopedMetricName) >= 1f);
    server.shutdown();
     
    val externalRequests :Collection[ExternalRequest]  = introspector.getExternalRequests(txOne);
    Assert.assertEquals(1, externalRequests.size());
    val externalRequest : ExternalRequest = externalRequests.iterator().next();
    Assert.assertEquals(1, externalRequest.getCount());
    Assert.assertEquals(host, externalRequest.getHostname());
    Assert.assertEquals("SprayClient", externalRequest.getLibrary());
    Assert.assertEquals("sendReceive", externalRequest.getOperation());     
    
  }
  
  val simplePipeline: HttpRequest => Future[HttpResponse] = (
    addHeader(HttpTestServer.SLEEP_MS_HEADER_KEY, "1000") ~>
    addHeader(HttpTestServer.DO_CAT, "false") ~>
    sendReceive)
  @Trace(dispatcher = true)
  def simpleTransaction( host :String) {
    val response: Future[HttpResponse] = simplePipeline(Get(host))
    Await.result(response, 15 seconds)
    Thread.sleep(5)
  }

  @Test
  def testNonTransaction() {
    val server :HttpTestServer = HttpServerLocator.createAndStart();
    val endpoint :URI = server.getEndPoint();
    val host :String  = endpoint.getHost();    
    nonTransaction(endpoint.toString());
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    server.shutdown()
  }

  def nonTransaction(host :String) {
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val response: Future[HttpResponse] = pipeline(Get(host))
    Await.result(response, 15 seconds)
    Thread.sleep(5)
  }
 
  @Test
  def testError() {
    try {
      errorTransaction("http://www.notarealhostbrosef.bro")
      Assert.fail("Host should not be reachable: http://www.notarealhostbrosef.bro");
    } catch {
      case ioe: RuntimeException =>
    }
    
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 1);
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
 
    var txOne :String = null;
    val it :java.util.Iterator[String] = introspector.getTransactionNames().iterator()
    while(it.hasNext()) {
      val txName :String = it.next();
      System.out.println(txName)
      if (txName.matches(".*SprayClientTest.*")) {
        txOne = txName;
      }
    }    
    
    // no metrics for unknown hosts in httpclient3
    Assert.assertEquals(0, MetricsHelper.getScopedMetricCount(txOne, "External/Unknown/CommonsHttp"));
    Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/Unknown/CommonsHttp"));

    // Unknown hosts generate no external rollups
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount( "External/" + "www.notarealhostbrosef.bro" + "/SprayClient/sendReceiveOnFailure"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther")); 
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));    
  }
  
  @Trace(dispatcher = true)
  def errorTransaction(host : String) {
    val response: Future[HttpResponse] = simplePipeline(Get(host))
    Await.result(response, 15 seconds)
    Thread.sleep(5)
  }
    
  // introspector does not handle async tx finishing very well so we're sleeping as a workaround
  private def awaitFinishedTx(introspector :Introspector, expectedTxCount: Int = 1) {
    while(introspector.getFinishedTransactionCount() <= expectedTxCount-1) {
      Thread.sleep(100)
    }
    Thread.sleep(100)
  }
}
