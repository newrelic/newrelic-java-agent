/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server

import org.apache.pekko.http.scaladsl.model.Uri.Path
import org.apache.pekko.http.scaladsl.server.PathMatcher.Matching
import org.apache.pekko.http.scaladsl.server.util.Tuple
import com.agent.instrumentation.org.apache.pekko.http.{PathMatcherScalaUtils, PathMatcherUtils}
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}

@Weave(`type` = MatchType.BaseClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatcher")
abstract class PathMatcher_Instrumentation[L] {

  def ev: Tuple[L] = Weaver.callOriginal()

  def |[R >: L : Tuple](other: PathMatcher[_ <: R]): PathMatcher[R] = {
    PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.appendPipe(), PathMatcherScalaUtils.emptyFunction2(), Weaver.callOriginal())
  }

  def unary_!(): PathMatcher0 = {
    val result: PathMatcher0 = Weaver.callOriginal()
    PathMatcherScalaUtils.pathMatcher0Wrapper(PathMatcherScalaUtils.appendNegation(), result)
  }

  def repeat(min: Int, max: Int, separator: PathMatcher0 = PathMatchers.Neutral)(implicit lift: PathMatcher.Lift[L, List]): PathMatcher[lift.Out] = {
    PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.startRepeat(), PathMatcherScalaUtils.endRepeat(),
      Weaver.callOriginal())(lift.OutIsTuple)
  }
}

@Weave(`type` = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatcher$")
abstract class PathMatcherObject_Instrumentation {
  def apply[L](prefix: Path, extractions: L, evidence: Tuple[L]): PathMatcher[L] = {
    PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.emptyFunction1(), PathMatcherScalaUtils.appendStaticString(prefix.toString()),
      Weaver.callOriginal())(evidence)
  }
}

@Weave(`type` = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatcher$EnhancedPathMatcher")
abstract class EnhancedPathMatcher[L](underlying: PathMatcher_Instrumentation[L]) {
  def ?(implicit lift: PathMatcher.Lift[L, Option]): PathMatcher[lift.Out] = {
    val result: PathMatcher[lift.Out] = Weaver.callOriginal()
    PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.appendOptional(), PathMatcherScalaUtils.emptyFunction2(), result)(lift.OutIsTuple)
  }
}

@Weave(`type` = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatcher$Matched")
class Matched_Instrumentation[L: Tuple](path: Path, extractions: L) {

  def pathRest: Path = {
    Weaver.callOriginal()
  }

  def andThen[R: Tuple](f: (Path, L) ⇒ Matching[R]): Matching[R] = {
    PathMatcherUtils.appendTilde(null)
    val returnValue = Weaver.callOriginal.asInstanceOf[Matching[R]]
    PathMatcherUtils.andThen(returnValue, pathRest)
    returnValue
  }

}
