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
import shapeless.HList;
import spray.http.Uri;

@Weave(originalName = "spray.routing.PathMatcher$PimpedPathMatcher$$anon$7")
public class OptionalPathMatcher {

    public PathMatcher.Matching<HList> apply(final Uri.Path path) {
        PathMatcherUtils.appendOptional(path);
        return Weaver.callOriginal();
    }

}
