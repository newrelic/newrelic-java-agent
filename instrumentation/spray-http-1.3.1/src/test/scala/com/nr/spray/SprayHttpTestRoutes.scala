/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.spray

import spray.routing.Directives._
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext.Implicits.global

object SprayHttpTestRoutes {
  val routes = {
    path("prefix-first") {
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
        ctx.complete("UUID: " + uuid.toString())
      }
    } ~
    path("uuid" / "future" / JavaUUID) { uuid =>
      onSuccess(Future {
        "UUID Future: " + uuid.toString()
      }) {
        result => complete(result)
      }
    } ~
    path("regex" / """\d+""".r) { digit =>
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
        get { (ctx: RequestContext) =>
          ctx.complete("PathEnd")
        }
      } ~
      path(Segment) { segment =>
        get { (ctx: RequestContext) =>
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
    path("restpath" / RestPath) { restpath =>
      get { (ctx: RequestContext) =>
        ctx.complete("RestPath: " + restpath)
      }
    } ~
    path("futurerestpath" / RestPath) { restpath =>
      onSuccess(Future {
        "FutureRestPath: " + restpath
      }) {
        result => complete(result)
      }
    } ~
    path("rest" ~ Rest) { rest =>
      get { (ctx: RequestContext) =>
        ctx.complete("Rest: " + rest)
      }
    } ~
    path("futurerest" ~ Rest) { rest =>
      onSuccess(Future {
        "FutureRest: " + rest
      }) {
        result => complete(result)
      }
    } ~
    path("int" / IntNumber) { number =>
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
    path("repeat" / IntNumber.repeat(separator = Slash) / "complex") { segments =>
      get { (ctx: RequestContext) =>
        val result = segments.mkString(",")
        ctx.complete("Repeat: " + result)
      }
    } ~
    path("futurerepeat" / IntNumber.repeat(separator = Slash) / "complex") { segments =>
      onSuccess(Future {
        val result = segments.mkString(",")
        "FutureRepeat: " + result
      }) {
        result => complete(result)
      }
    } ~
    path("pipe" / ("i" ~ IntNumber | "h" ~ HexIntNumber)) { number =>
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
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
      get { (ctx: RequestContext) =>
        ctx.complete("Negation")
      }
    } ~
    pathPrefix("futurematch" ~ !"nomatch") {
      onSuccess(Future {
        "FutureNegation"
      }) {
        result => complete(result)
      }
    }
  }
}
