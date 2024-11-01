/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server;

import org.apache.pekko.http.scaladsl.model.Uri;
import com.agent.instrumentation.org.apache.pekko.http.PathMatcherUtils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Tuple1;
import scala.runtime.BoxedUnit;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatchers")
public class PekkoHttpPathMatchers {

    @Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatchers$Slash$")
    public static class PekkoHttpSlash$ {

        public PathMatcher.Matching<BoxedUnit> apply(final Uri.Path path) {
            PathMatcher.Matching<BoxedUnit> matching = Weaver.callOriginal();
            PathMatcherUtils.appendSlash(path, matching);
            return matching;
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatchers$Remaining$")
    public static class PekkoHttpRemaining$ {

        public PathMatcher.Matching<Tuple1<String>> apply(final Uri.Path path) {
            PathMatcher.Matched<Tuple1<String>> matched = Weaver.callOriginal();
            PathMatcherUtils.appendRemaining("Remaining", path, matched);
            return matched;
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatchers$RemainingPath$")
    public static class PekkoHttpRemainingPath$ {

        public PathMatcher.Matching<Tuple1<String>> apply(final Uri.Path path) {
            PathMatcher.Matched<Tuple1<String>> matched = Weaver.callOriginal();
            PathMatcherUtils.appendRemaining("RemainingPath", path, matched);
            return matched;
        }

    }

    @Weave(type = MatchType.BaseClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatchers$NumberMatcher")
    public static class PekkoHttpNumberMatcher<T> {

        public PathMatcher.Matching<Tuple1<T>> apply(final Uri.Path path) {
            PathMatcher.Matching<Tuple1<T>> matching = Weaver.callOriginal();
            PathMatcherUtils.appendNumberMatch(getClass().getSimpleName().replaceAll("\\$", ""), path, matching);
            return matching;
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.server.PathMatchers$Segment$")
    public static class PekkoHttpSegment$ {

        public PathMatcher.Matching<Tuple1<String>> apply(final Uri.Path path) {
            PathMatcher.Matching<Tuple1<String>> matching = Weaver.callOriginal();
            PathMatcherUtils.appendSegment(path, matching);
            return matching;
        }
    }
}
