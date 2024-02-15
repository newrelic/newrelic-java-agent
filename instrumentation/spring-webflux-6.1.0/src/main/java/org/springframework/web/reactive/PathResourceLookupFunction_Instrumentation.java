/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;

@Weave(originalName = "org.springframework.web.reactive.function.server.PathResourceLookupFunction")
class PathResourceLookupFunction_Instrumentation {
    private final PathPattern pattern = Weaver.callOriginal();

    private final Resource location = Weaver.callOriginal();

    public Mono<Resource> apply(ServerRequest request) {
        Mono<Resource> result = Weaver.callOriginal();
        if (!Mono.empty().equals(result)) {
            Util.addPath(request, pattern.getPatternString());
        }
        return result;
    }
}
