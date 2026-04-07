package com.nr.http4s

import cats.effect.IO
import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector, TransactionEvent, TransactionTrace}
import org.http4s.Method.GET
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.HttpRoutes
import org.junit.runner.RunWith

import scala.jdk.CollectionConverters._
import org.junit.{After, Assert, Before, Test}

import java.net.URL
import scala.io.Source
import scala.util.{Try, Using}
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("org.http4s"))
class BlazeServerBuilderTest {

  val blazeServer = new Http4sTestServer(HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }.orNotFound)

  @Before
  def setup() = {
    blazeServer.start()
  }

  @After
  def reset(): Unit = {
    blazeServer.stop()
  }

  def get(hostname: String, port: Int, path: String): Try[String] =
    Using(
      Source.fromURL(new URL(s"http://$hostname:$port$path"))
    )(_.getLines().mkString)


  @Test
  def blazeServerTest: Unit = {

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val request = get(blazeServer.hostname, blazeServer.port, "/hello/bob")

    val txnCount = introspector.getFinishedTransactionCount()
    val webTxnName = introspector.getTransactionNames.asScala.headOption
    val traces = getTraces(introspector)
    Assert.assertTrue("Web request successful", request.isSuccess)
    Assert.assertEquals("Transaction count correct", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertEquals("Transaction name correct", Some("WebTransaction/HTTP4s/BlazeServerHandler"), webTxnName)

    val txns = introspector.getTransactionEvents("WebTransaction/HTTP4s/BlazeServerHandler")
    txns.forEach(assertWebAttributes)
  }

  @Test
  def blazeServerTestMany: Unit = {
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
    
    val requestCount = 1000
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val requestsSent = Future.sequence((0 until requestCount).toList.map(_ => Future(get(blazeServer.hostname, blazeServer.port, "/hello/bob"))))

    val requests = Await.result(requestsSent, Duration("1 minute"))
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    Assert.assertEquals("Transaction count correct", requestCount, txnCount)
    Assert.assertEquals("Trace present", requestCount, traces.size)
  }


  private def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  private def assertWebAttributes(t: TransactionEvent): Unit = {
    //original implementations of the server instrumentation did not include response attributes
    val actualAttrs: scala.collection.mutable.Map[String, AnyRef] = t.getAttributes.asScala

    val expectedReqAttrs = List("request.uri", "request.method")
    val expectedResAttrs = List("httpResponseCode", "http.statusCode") //don't care whether we send standard or legacy, but one of these should be there
    Assert.assertTrue("Transaction should include web request attributes", expectedReqAttrs.forall(actualAttrs.contains))
    Assert.assertTrue("Transaction should include web response attributes", expectedResAttrs.exists(actualAttrs.contains))
  }
}
