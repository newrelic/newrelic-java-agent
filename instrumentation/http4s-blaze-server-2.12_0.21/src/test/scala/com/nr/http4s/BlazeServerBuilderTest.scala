package com.nr.http4s

import cats.effect.{IO, Resource}
import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector, TransactionTrace}
import org.http4s.Method.GET
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.HttpRoutes
import org.junit.runner.RunWith

import collection.JavaConverters._
import org.junit.{After, Assert, Before, Test}

import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.io.Source

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("org.http4s", "cats.effect"))
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

  def get(hostname: String, port: Int, path: String): Option[String] =
    Resource.fromAutoCloseable(IO(Source.fromURL(new URL(s"http://$hostname:$port$path"))))
            .use(body => IO(body.getLines().mkString))
            .unsafeRunTimed(2.seconds)

  @Test
  def blazeServerTest: Unit = {

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val request = get(blazeServer.hostname, blazeServer.port, "/hello/bob")

    val txnCount = introspector.getFinishedTransactionCount()
    val webTxnName = introspector.getTransactionNames.asScala.headOption
    val traces = getTraces(introspector)
    Assert.assertTrue("Web request successful", request.isDefined)
    Assert.assertEquals("Transaction count correct", 1, txnCount)
    Assert.assertEquals("Trace present", 1, traces.size)
    Assert.assertEquals("Transaction name correct", Some("WebTransaction/HTTP4s/BlazeServerHandler"), webTxnName)

  }


  private def getTraces(introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

}
