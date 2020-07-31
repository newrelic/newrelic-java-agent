/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.routing;

import com.agent.instrumentation.spray.PathMatcherUtils;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.util.matching.Regex;
import shapeless.$colon$colon;
import shapeless.HNil;
import spray.http.Uri;

@Weave(originalName = "spray.routing.ImplicitPathMatcherConstruction$$anon$9")
public class RegexPathMatcher2 {

    private final Regex regex$1 = Weaver.callOriginal();

    public PathMatcher.Matching<$colon$colon<String, HNil>> apply(final Uri.Path path) {
        PathMatcher.Matching<$colon$colon<String, HNil>> matching = Weaver.callOriginal();
        PathMatcherUtils.appendRegex(path, regex$1.pattern().toString(), matching);
        return matching;
    }

}
