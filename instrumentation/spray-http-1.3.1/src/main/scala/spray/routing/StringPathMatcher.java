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

@Weave(originalName = "spray.routing.PathMatcher$$anon$6")
public class StringPathMatcher {

    private final Uri.Path prefix$1 = Weaver.callOriginal();

    public PathMatcher.Matching<HNil> apply(final Uri.Path path) {
        PathMatcher.Matching<HNil> matching = Weaver.callOriginal();
        PathMatcherUtils.appendStaticString(path, prefix$1.toString(), matching);
        return matching;
    }

}
