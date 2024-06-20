/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.server

import com.agent.instrumentation.akka.http102.PathMatcherScalaUtils
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}

import scala.util.matching.Regex

@Weave(`type` = MatchType.BaseClass, originalName = "akka.http.scaladsl.server.ImplicitPathMatcherConstruction")
abstract class ImplicitPathMatcherConstruction_Instrumentation {
  implicit def _regex2PathMatcher(regex: Regex): PathMatcher1[String] = {
    PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.emptyFunction1(), PathMatcherScalaUtils.appendRegex(regex), Weaver.callOriginal())
  }
}
