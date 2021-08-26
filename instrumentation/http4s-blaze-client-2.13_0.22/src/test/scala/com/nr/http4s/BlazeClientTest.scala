package com.nr.http4s

import cats.effect.{ContextShift, IO, Timer}
import com.newrelic.agent.introspec.internal.HttpServerRule
import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner, Introspector}
import com.nr.http4s.Http4sTestUtils.{getSegments, getTraces, makeRequest}
import org.junit.runner.RunWith
import org.junit.{Assert, Rule, Test}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt


@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("org.http4s", "com.newrelic.cats.api"))
class BlazeClientTest {

  @Rule
  def server: HttpServerRule = _server

  implicit val ec: ExecutionContext = global
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val _server = new HttpServerRule()

  @Test
  def blazeClientTest: Unit = {

    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    val response = makeRequest(s"${server.getEndPoint}?no-transaction=1").unsafeRunTimed(2.seconds)


    val traces = getTraces(introspector)
    val segments = getSegments(traces)
    val txnCount = introspector.getFinishedTransactionCount()
    Assert.assertTrue("Web request successful", response.isDefined)
  }
}
