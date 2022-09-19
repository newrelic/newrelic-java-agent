package com.nr.http4s

import cats.effect.IO
import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector, TransactionTrace}
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
class EmberServerBuilderTest {

  val testServerHost = "0.0.0.0"
  val emberServer = new Http4sTestServer(testServerHost,
    HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
    }.orNotFound)

  @Before
  def setup() = {
    emberServer.start()
  }

  @After
  def reset(): Unit = {
    emberServer.stop()
  }

  def get(hostname: String, port: Int, path: String): Try[String] =
    Using(
      Source.fromURL(new URL(s"http://$hostname:$port$path"))
    )(_.getLines().mkString)


  @Test
  def emberServerTest: Unit = {

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val request = get(testServerHost, emberServer.port, "/hello/bob")

    val txnCount = introspector.getFinishedTransactionCount()
    val webTxnName = introspector.getTransactionNames.asScala.headOption
    val traces = getTraces(introspector)
    Assert.assertTrue("Web request successful", request.isSuccess)
    Assert.assertEquals("Transaction count correct", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertEquals("Transaction name correct", Some("WebTransaction/HTTP4s/EmberServerHandler"), webTxnName)

  }

  @Test
  def emberServerTestMany: Unit = {
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

    val requestCount = 1000
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val requestsSent = Future.sequence((0 until requestCount).toList.map(_ => Future(get(testServerHost, emberServer.port,
      "/hello/bob"))))

    val requests = Await.result(requestsSent, Duration("1 minute"))
    val txnCount = introspector.getFinishedTransactionCount()
    val traces = getTraces(introspector)
    Assert.assertEquals("Transaction count correct", requestCount, txnCount)
    Assert.assertEquals("Trace present", requestCount, traces.size)
  }


  private def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

}
