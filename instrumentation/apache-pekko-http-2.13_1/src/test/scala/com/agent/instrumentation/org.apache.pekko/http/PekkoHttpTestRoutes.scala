/*
 *
 *  Copyright 2024 New Relic Corporation. All rights reserved.
 *  SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http

import org.apache.pekko.actor.{ActorRef, ActorSystem, Scheduler}
import org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server._
import org.apache.pekko.http.scaladsl.server.directives.BasicDirectives
import org.apache.pekko.pattern.{after, ask}
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.util.{ByteString, Timeout}
import com.agent.instrumentation.org.apache.pekko.http.StatusCheckActor.Ping
import com.newrelic.api.agent.NewRelic

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.language.postfixOps

class PekkoHttpTestRoutes {

  implicit val system: ActorSystem = ActorSystem("pekkohttptest")
  implicit val scheduler: Scheduler = system.scheduler
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val requestTimeout: Timeout = Timeout(30 seconds)

  val newrelicCheck: ActorRef = system.actorOf(StatusCheckActor.props, "StatusCheck")

  def customTx(s: String): Directive0 = mapResponse { resp =>
    NewRelic.addCustomParameter("attr1", "newrelic-test")
    NewRelic.addCustomParameter("attr2", s)
    resp
  }

  def customDirective: Directive1[Option[String]] = {
    optionalHeaderValueByName("X-NewRelic-Directive").flatMap {
      case Some(directiveHeader) =>
        onComplete(AsyncApp.asyncTest(directiveHeader)).flatMap {
          case Success(result) => BasicDirectives.provide(result)
          case _ =>
            BasicDirectives.provide(None)
        }
      case None => BasicDirectives.provide(None)
    }
  }

  def pekkoStreamDirective: Directive1[String] = {
    extractDataBytes.flatMap { data =>
      val body = data.runFold(ByteString.empty)(_ ++ _).map(_.utf8String)

      onSuccess(body).flatMap { payload =>
        provide(payload)
      }
    }
  }

  def hostnameAndPort: Directive[(String, Int)] = Directive[(String, Int)] { inner =>
    ctx =>
      val authority = ctx.request.uri.authority
      inner((authority.host.address(), authority.port))(ctx)
  }

  val routes: Route = rejectEmptyResponse {
    customDirective { _: Option[String] =>
      pathPrefix("custom-directive") {
        pathPrefix("v2") {
          pathPrefix("docs") {
            parameters(Symbol("parameter")) { parameterId: String =>
              get { ctx: RequestContext =>
                ctx.complete("CustomDirectiveDocsParam: " + parameterId)
              }
            } ~
              path(Segment) { segment =>
                get { ctx: RequestContext =>
                  ctx.complete("CustomDirectiveDocsSegment: " + segment)
                }
              } ~
              path(Segment / "details") { segment =>
                get { ctx: RequestContext =>
                  ctx.complete("CustomDirectiveDocsSegmentDetails: " + segment)
                }
              } ~
              path(Segment / "details" / "test") { segment =>
                get { ctx: RequestContext =>
                  ctx.complete("CustomDirectiveDocsSegmentDetailsTest: " + segment)
                }
              }
          }
        }
      } ~
        path("callid" / Segment) { id =>
          val fut: Future[String] = (newrelicCheck ? Ping(id.toInt)).map {
            case str: String => s"${str}_id"
            case _ => "ERROR"
          }

          onSuccess(fut) { response =>
            customTx("test") {
              respondWithHeaders(RawHeader("Test-Header", "async-directive")) {
                complete(response)
              }
            }
          }
        } ~
        pekkoStreamDirective { implicit data => {
          path("prefix-first") {
            get { ctx: RequestContext =>
              ctx.complete("prefix-first")
            }
          } ~
            path("prefix-first-future") {
              onSuccess(Future {
                "prefix-first-future"
              }) {
                result => complete(result)
              }
            } ~
            path("prefix-first-second") {
              get { ctx: RequestContext =>
                ctx.complete("prefix-first-second")
              }
            } ~
            path("prefix-first-second-future") {
              onSuccess(Future {
                "prefix-first-second-future"
              }) {
                result => complete(result)
              }
            } ~
            path("simple" / "route") {
              get { ctx: RequestContext =>
                ctx.complete("Simple Route")
              }
            } ~
            path("simple" / "route" / "future") {
              onSuccess(Future {
                "Simple Route Future"
              }) {
                result => complete(result)
              }
            } ~
            path("uuid" / JavaUUID) { uuid =>
              get { ctx: RequestContext =>
                ctx.complete("UUID: " + uuid.toString)
              }
            } ~
            path("uuid" / "future" / JavaUUID) { uuid =>
              onSuccess(Future {
                "UUID Future: " + uuid.toString
              }) {
                result => complete(result)
              }
            } ~
            path("regex" / """\d+""".r) { digit =>
              get { ctx: RequestContext =>
                ctx.complete("Regex: " + digit)
              }
            } ~
            path("regex" / "future" / """\d+""".r) { digit =>
              onSuccess(Future {
                "Regex Future: " + digit
              }) {
                result => complete(result)
              }
            } ~
            path("map" / Map("red" -> 1, "green" -> 2, "blue" -> 3)) { value =>
              get { ctx: RequestContext =>
                ctx.complete("Map: " + value)
              }
            } ~
            path("map" / "future" / Map("red" -> 1, "green" -> 2, "blue" -> 3)) { value =>
              onSuccess(Future {
                "Map Future: " + value
              }) {
                result => complete(result)
              }
            } ~
            path("segment" / "foo" ~ Segment) { segment =>
              get { ctx: RequestContext =>
                ctx.complete("Segment: " + segment)
              }
            } ~
            path("segment" / "future" / "foo" ~ Segment) { segment =>
              onSuccess(Future {
                "Segment Future: " + segment
              }) {
                result => complete(result)
              }
            } ~
            pathPrefix("pathend") {
              pathEnd {
                get { ctx: RequestContext =>
                  ctx.complete("PathEnd")
                }
              } ~
                path(Segment) { segment =>
                  get { ctx: RequestContext =>
                    ctx.complete("PathEnd: " + segment)
                  }
                }
            } ~
            pathPrefix("pathendfuture") {
              pathEnd {
                onSuccess(Future {
                  "PathEndFuture"
                }) {
                  result => complete(result)
                }
              } ~
                path(Segment) { segment =>
                  onSuccess(Future {
                    "PathEndFuture: " + segment
                  }) {
                    result => complete(result)
                  }
                }
            } ~
            path("remainingpath" / RemainingPath) { remainingpath =>
              get { ctx: RequestContext =>
                ctx.complete("RemainingPath: " + remainingpath)
              }
            } ~
            path("futureremainingpath" / RemainingPath) { remainingpath =>
              onSuccess(Future {
                "FutureRemainingPath: " + remainingpath
              }) {
                result => complete(result)
              }
            } ~
            path("remain" ~ Remaining) { remaining =>
              get { ctx: RequestContext =>
                ctx.complete("Remain: " + remaining)
              }
            } ~
            path("futureremain" ~ Remaining) { remaining =>
              onSuccess(Future {
                "FutureRemain: " + remaining
              }) {
                result => complete(result)
              }
            } ~
            path("int" / IntNumber) { number =>
              get { ctx: RequestContext =>
                ctx.complete("IntNumber: " + number)
              }
            } ~
            path("intfuture" / IntNumber) { number =>
              onSuccess(Future {
                "IntNumberFuture: " + number
              }) {
                result => complete(result)
              }
            } ~
            path("long" / LongNumber) { number =>
              get { ctx: RequestContext =>
                ctx.complete("LongNumber: " + number)
              }
            } ~
            path("longfuture" / LongNumber) { number =>
              onSuccess(Future {
                "LongNumberFuture: " + number
              }) {
                result => complete(result)
              }
            } ~
            path("hexint" / HexIntNumber) { number =>
              get { ctx: RequestContext =>
                ctx.complete("HexIntNumber: " + number)
              }
            } ~
            path("hexintfuture" / HexIntNumber) { number =>
              onSuccess(Future {
                "HexIntNumberFuture: " + number
              }) {
                result => complete(result)
              }
            } ~
            path("hexlong" / HexLongNumber) { number =>
              get { ctx: RequestContext =>
                ctx.complete("HexLongNumber: " + number)
              }
            } ~
            path("hexlongfuture" / HexLongNumber) { number =>
              onSuccess(Future {
                "HexLongNumberFuture: " + number
              }) {
                result => complete(result)
              }
            } ~
            path("double" / DoubleNumber) { number =>
              get { ctx: RequestContext =>
                ctx.complete("DoubleNumber: " + number)
              }
            } ~
            path("double" / "future" / DoubleNumber) { number =>
              onSuccess(Future {
                "DoubleNumberFuture: " + number
              }) {
                result => complete(result)
              }
            } ~
            path("segments" / Segments) { segments =>
              get { ctx: RequestContext =>
                val result = segments.mkString(",")
                ctx.complete("Segments: " + result)
              }
            } ~
            path("futuresegments" / Segments) { segments =>
              onSuccess(Future {
                val result = segments.mkString(",")
                "FutureSegments: " + result
              }) {
                result => complete(result)
              }
            } ~
            path("repeat" / IntNumber.repeat(2, 3, separator = Slash) / "complex") { segments =>
              get { ctx: RequestContext =>
                val result = segments.mkString(",")
                ctx.complete("Repeat: " + result)
              }
            } ~
            path("futurerepeat" / IntNumber.repeat(2, 3, separator = Slash) / "complex") { segments =>
              onSuccess(Future {
                val result = segments.mkString(",")
                "FutureRepeat: " + result
              }) {
                result => complete(result)
              }
            } ~
            path("zerorepeat" / IntNumber.repeat(0, 2, separator = Slash)) { segments =>
              get { ctx: RequestContext =>
                val result = segments.mkString(",")
                ctx.complete("ZeroRepeat: " + result)
              }
            } ~
            path("pipe" / ("i" ~ IntNumber | "h" ~ HexIntNumber)) { number =>
              get { ctx: RequestContext =>
                ctx.complete("Pipe: " + number)
              }
            } ~
            path("futurepipe" / ("i" ~ IntNumber | "h" ~ HexIntNumber)) { number =>
              onSuccess(Future {
                "FuturePipe: " + number
              }) {
                result => complete(result)
              }
            } ~
            path("pipe" / "optional" / "X" ~ IntNumber.? / ("edit" | "create")) { number =>
              get { ctx: RequestContext =>
                ctx.complete("Pipe + Optional: " + number.orNull)
              }
            } ~
            path("futurepipe" / "optional" / "X" ~ IntNumber.? / ("edit" | "create")) { number =>
              onSuccess(Future {
                "FuturePipe + Optional: " + number.orNull
              }) {
                result => complete(result)
              }
            } ~
            pathPrefix("match" ~ !"nomatch") {
              get { ctx: RequestContext =>
                ctx.complete("Negation")
              }
            } ~
            pathPrefix("futurematch" ~ !"nomatch") {
              onSuccess(Future {
                "FutureNegation"
              }) {
                result => complete(result)
              }
            } ~
            pathPrefix("v1") {
              pathPrefix("containers") {
                parameters(Symbol("parameter")) { parameterId: String =>
                  get { ctx: RequestContext =>
                    ctx.complete("ContainersParam: " + parameterId)
                  }
                } ~
                  path(Segment) { segment =>
                    get { ctx: RequestContext =>
                      ctx.complete("ContainersSegment: " + segment)
                    }
                  } ~
                  path(Segment / "details") { segment =>
                    get { ctx: RequestContext =>
                      ctx.complete("ContainersSegmentDetails: " + segment)
                    }
                  } ~
                  path(Segment / "details" / "test") { segment =>
                    get { ctx: RequestContext =>
                      ctx.complete("ContainersSegmentDetailsTest: " + segment)
                    }
                  }
              }
            } ~
            path("future" / IntNumber) { millis =>
              val futureResult = after(millis.millis, scheduler)(Future.successful(StatusCodes.OK))
              complete(ToResponseMarshallable.apply(futureResult))
            } ~
            path("hostandport") {
              hostnameAndPort {
                (_, _) => complete(StatusCodes.OK)
              }
            } ~
            path("test-error") {
              throw new UnsupportedOperationException
            } ~
            path("test-error-2") {
              complete(StatusCodes.InternalServerError, "ErrorTest")
            } ~
            pathPrefix("path-end") {
              path(Remaining) {
                path => {
                  complete(StatusCodes.OK, "Remaining: " + path)
                }
              } ~
                pathEnd {
                  get {
                    complete(StatusCodes.OK, "Get path end!")
                  }
                }
            } ~
            pathPrefix("path-prefix-end") {
              path("first-case") {
                complete(StatusCodes.OK, "First case")
              } ~
                path(Remaining) {
                  path => {
                    complete(StatusCodes.OK, "Remaining: " + path)
                  }
                } ~
                pathEnd {
                  get {
                    complete(StatusCodes.OK, "Get path end!")
                  }
                }
            }
        }
        }
    }
  }
}
