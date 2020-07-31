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
import scala.Function2;
import shapeless.HList;
import spray.http.Uri;

@Weave(type = MatchType.Interface, originalName = "spray.routing.PathMatcher")
public abstract class SprayPathMatcher {

    @Weave(type = MatchType.Interface, originalName = "spray.routing.PathMatcher$Matching")
    public static class SprayMatching<L extends HList>{

    }

    @Weave(type = MatchType.ExactClass, originalName = "spray.routing.PathMatcher$Matched")
    public static class SprayMatched<L extends HList> {

        public <R extends HList> PathMatcher.Matching<R> andThen(final Function2<Uri.Path, L, PathMatcher.Matching<R>> f) {
            PathMatcherUtils.appendTilde(null);
            PathMatcher.Matching<R> returnValue = Weaver.callOriginal();
            PathMatcherUtils.andThen(returnValue);
            return returnValue;
        }

        public Uri.Path pathRest() {
            return Weaver.callOriginal();
        }

    }

    @Weave(originalName = "spray.routing.PathMatcher$Unmatched$")
    public static class SprayUnmatched$ {
        
    }

    @Weave(originalName = "spray.routing.PathMatcher$Lift")
    public static class SprayLift<L extends HList, M> {
        
    }
}
