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
import shapeless.HNil;
import spray.http.Uri;

@Weave(originalName = "spray.routing.PathMatcher$$anon$2")
public class NegationPathMatcher {

    public PathMatcher.Matching<HNil> apply(final Uri.Path path) {
        PathMatcherUtils.appendNegation(path);
        PathMatcher.Matching<HNil> matching = Weaver.callOriginal();
        return matching;
    }

}

