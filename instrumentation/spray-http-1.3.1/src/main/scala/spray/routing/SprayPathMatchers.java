/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.routing;

import com.agent.instrumentation.spray.PathMatcherUtils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import shapeless.HNil;
import spray.http.Uri;

@Weave(type = MatchType.ExactClass, originalName = "spray.routing.PathMatchers")
public class SprayPathMatchers {

    @Weave(type = MatchType.ExactClass, originalName = "spray.routing.PathMatchers$Slash$")
    public static class SpraySlash$ {

        public PathMatcher.Matching<HNil> apply(final Uri.Path path) {
            PathMatcher.Matching<HNil> matching = Weaver.callOriginal();
            PathMatcherUtils.appendSlash(path, matching);
            return matching;
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "spray.routing.PathMatchers$Rest$")
    public static class SprayRest$ {

        public PathMatcher.Matched<HNil> apply(final Uri.Path path) {
            PathMatcher.Matched<HNil> matched = Weaver.callOriginal();
            PathMatcherUtils.appendRest("Rest", path, matched);
            return matched;
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "spray.routing.PathMatchers$RestPath$")
    public static class SprayRestPath$ {

        public PathMatcher.Matched<HNil> apply(final Uri.Path path) {
            PathMatcher.Matched<HNil> matched = Weaver.callOriginal();
            PathMatcherUtils.appendRest("RestPath", path, matched);
            return matched;
        }

    }

    @Weave(type = MatchType.BaseClass, originalName = "spray.routing.PathMatchers$NumberMatcher")
    public static class SprayNumberMatcher {

        public PathMatcher.Matching<HNil> apply(final Uri.Path path) {
            PathMatcher.Matching<HNil> matching = Weaver.callOriginal();
            PathMatcherUtils.appendNumberMatch(getClass().getSimpleName().replaceAll("\\$", ""), path, matching);
            return matching;
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "spray.routing.PathMatchers$Segment$")
    public static class SpraySegment$ {

        public PathMatcher.Matching<HNil> apply(final Uri.Path path) {
            PathMatcher.Matching<HNil> matching = Weaver.callOriginal();
            PathMatcherUtils.appendSegment(path, matching);
            return matching;
        }
    }
}