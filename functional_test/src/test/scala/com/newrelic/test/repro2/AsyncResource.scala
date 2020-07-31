/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.repro2

import java.util.concurrent.atomic.AtomicInteger

import com.ning.http.client.AsyncHttpClient
import javax.ws.rs.Produces
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod, RestController}
import org.springframework.web.context.request.async.DeferredResult

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SpringBootApplication
@Component
@RestController
class AsyncResource {

  val counter = new AtomicInteger()

  val asyncHttpClient = new AsyncHttpClient()

  implicit val ec = ExecutionContext.fromExecutorService(SpringBootAsyncTest.executorService)

  @RequestMapping(value = Array("/{port}"), method = Array(RequestMethod.GET))
  @Produces(Array("application/json"))
  @Async
  def asyncTest(@PathVariable port: String): DeferredResult[Foo] = {
    val count = counter.incrementAndGet()
    val result = new DeferredResult[Foo](60000L)
    val barF = Future(bar(Integer.valueOf(port)))
    val bazF = Future(baz(Integer.valueOf(port)))
    val resF =
      for {
        bar <- barF
        baz <- bazF
      } yield Foo(bar, baz)

    resF onComplete {
      case Success(r) => result.setResult(r);
      case Failure(e) => result.setErrorResult(e);
    }
    result
  }

  private def bar(port: Int) = {
    val result = asyncHttpClient.prepareGet("http://localhost:" + port + "?no-transaction=true").execute().get
    result.getStatusText
  }

  private def baz(port: Int) = {
    val result = asyncHttpClient.prepareGet("http://localhost:" + port + "?no-transaction=true").execute().get
    result.getStatusText
  }

}

case class Foo(bar: String, baz: String)

