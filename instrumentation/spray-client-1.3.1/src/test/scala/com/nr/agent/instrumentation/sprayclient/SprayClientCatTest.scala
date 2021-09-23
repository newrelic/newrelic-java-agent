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
class SprayClientCatTest {
  implicit val system = akka.actor.ActorSystem()
  import system.dispatcher // execution context
 
  @Test
  def testCat() {
    val server :HttpTestServer = HttpServerLocator.createAndStart();
    val endpoint :URI = server.getEndPoint();
    val host :String  = endpoint.getHost();    
    catTransaction(endpoint.toString())
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 2);

    var txOne :String = null;
    val it :java.util.Iterator[String] = introspector.getTransactionNames().iterator()
    while(it.hasNext()) {
      val txName :String = it.next();
      if (txName.matches(".*SprayClientCatTest.*")) {
        txOne = txName;
      }
    }
    Assert.assertNotNull("Unable to find transaction", txOne);

    // Transaction
    Assert.assertEquals(2, introspector.getFinishedTransactionCount());
    val names : Collection[String] = introspector.getTransactionNames();
    Assert.assertEquals(2, names.size());
    Assert.assertTrue(names.contains(server.getServerTransactionName()));
    Assert.assertTrue(names.contains(txOne));    
    
    // unscoped metrics
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
    
    // scoped metrics
    val scopedMetricName :String = "ExternalTransaction/" + host + "/" +
                                   server.getCrossProcessId() + "/" + server.getServerTransactionName();
    Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, scopedMetricName));
    Assert.assertTrue("Expected external segment of one second or greater for scoped metric: "+scopedMetricName,
    MetricsHelper.getScopedMetricExclusiveTimeInSec(txOne, scopedMetricName) >= 1f);
    
    // events
    val transactionEvents :Collection[TransactionEvent]  = introspector.getTransactionEvents(txOne);
    Assert.assertEquals(1, transactionEvents.size());
    val transactionEvent : TransactionEvent = transactionEvents.iterator().next();
    Assert.assertEquals(1, transactionEvent.getExternalCallCount());
    Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);
   
    // Verify CAT
    CatHelper.verifyOneSuccessfulCat(introspector, txOne);
    
    // external request information
    val externalRequests :Collection[ExternalRequest] = introspector.getExternalRequests(txOne);
    Assert.assertEquals(1, externalRequests.size());
    val externalRequest : ExternalRequest = externalRequests.iterator().next();
    Assert.assertEquals(1, externalRequest.getCount());
    Assert.assertEquals(host, externalRequest.getHostname());
    
    // shutdown server
    server.shutdown();
  }
  
  val simplePipeline: HttpRequest => Future[HttpResponse] = ( addHeader(HttpTestServer.SLEEP_MS_HEADER_KEY, "1000") ~> sendReceive )
  @Trace(dispatcher = true)
  def catTransaction(host : String) {
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
