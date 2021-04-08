/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.akka.http

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.util.Tuple
import akka.http.scaladsl.server.{PathMatcher, PathMatcher0}
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.weaver.Weaver

import scala.util.matching.Regex

object PathMatcherScalaUtils {

  def pathMatcher0Wrapper(runBefore: Path => Unit, original: PathMatcher0): PathMatcher0 = {
    new PathMatcher[Unit] {
      def apply(path: Path): PathMatcher.Matching[Unit] = {
        try {
          runBefore.apply(path)
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }
        original.apply(path)
      }
    }
  }

  def pathMatcherWrapper[L](runBefore: Path => Unit, runAfter: (Path, PathMatcher.Matching[L]) => Unit, original: PathMatcher[L])(implicit ev: Tuple[L]): PathMatcher[L] = {
    new PathMatcher[L] {
      override def apply(path: Path): PathMatcher.Matching[L] = {
        try {
          runBefore.apply(path)
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }
        val result = original.apply(path)
        try {
          runAfter.apply(path, result)
        } catch {
          case t: Throwable => AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        }
        result
      }
    }
  }

  def appendNegation(): Path => Unit = {
    (path: Path) => PathMatcherUtils.appendNegation()
  }

  def appendOptional(): Path => Unit = {
    (path: Path) => PathMatcherUtils.appendOptional()
  }

  def appendPipe(): Path => Unit = {
    (path: Path) => PathMatcherUtils.appendPipe(path)
  }

  def appendRegex[L](regex: Regex): (Path, PathMatcher.Matching[L]) => Unit = {

    (path: Path, matching: PathMatcher.Matching[L]) => {
      val nrRequestContext = PathMatcherUtils.nrRequestContext.get()
      if (matching.isInstanceOf[PathMatcher.Matched[L]] && nrRequestContext != null && !nrRequestContext.regexHolder.contains(regex.toString())) {
        PathMatcherUtils.appendRegex(path, regex.pattern.toString, matching)
        nrRequestContext.regexHolder.add(regex.toString())
      }
    }
  }

  def startRepeat(): Path => Unit = {
    (path: Path) => PathMatcherUtils.startRepeat(path)
  }

  def endRepeat[L](): (Path, PathMatcher.Matching[L]) => Unit = {
    (path: Path, matching: PathMatcher.Matching[L]) => {
      PathMatcherUtils.endRepeat(path, matching)
    }
  }

  def appendStaticString[L](prefix: String): (Path, PathMatcher.Matching[L]) => Unit = {
    (path: Path, matching: PathMatcher.Matching[L]) => {
      PathMatcherUtils.appendStaticString(path, prefix, matching)
    }
  }

  def emptyFunction1(): Path => Unit = {
    (path: Path) => ()
  }

  def emptyFunction2[L](): (Path, PathMatcher.Matching[L]) => Unit = {
    (path: Path, pathMatcher: PathMatcher.Matching[L]) => ()
  }
}
