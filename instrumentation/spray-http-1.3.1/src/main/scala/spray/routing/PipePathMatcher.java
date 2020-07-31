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
import spray.http.Uri;

@Weave(originalName = "spray.routing.PathMatcher$$anon$1")
public class PipePathMatcher {

    public PathMatcher.Matching<?> apply(final Uri.Path path) {
        PathMatcherUtils.appendPipe(path);
        PathMatcher.Matching<?> matching = Weaver.callOriginal();
        return matching;
    }

}
