/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server

import com.agent.instrumentation.org.apache.pekko.http.PathMatcherScalaUtils
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}

import scala.util.matching.Regex

@Weave(`type` = MatchType.BaseClass, originalName = "org.apache.pekko.http.scaladsl.server.ImplicitPathMatcherConstruction")
abstract class ImplicitPathMatcherConstruction_Instrumentation {
  implicit def _regex2PathMatcher(regex: Regex): PathMatcher1[String] = {
    PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.emptyFunction1(), PathMatcherScalaUtils.appendRegex(regex), Weaver.callOriginal())
  }
}
