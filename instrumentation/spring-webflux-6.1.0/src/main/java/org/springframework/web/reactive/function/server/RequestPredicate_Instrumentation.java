/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.server;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Optional;
import java.util.function.Predicate;

@Weave(originalName = "org.springframework.web.reactive.function.server.RequestPredicates")
public class RequestPredicate_Instrumentation {

    @Weave(originalName = "org.springframework.web.reactive.function.server.RequestPredicates$AndRequestPredicate")
    static class AndRequestPredicate_Instrumentation {
        private final RequestPredicate left = Weaver.callOriginal();

        private final RequestPredicate right = Weaver.callOriginal();

        protected RequestPredicates.RequestModifyingPredicate.Result testInternal(ServerRequest request) {
            RequestPredicates.RequestModifyingPredicate.Result result = Weaver.callOriginal();
            boolean matched = result.value();

            if (matched && right != null && !(right instanceof HeadersPredicate_Instrumentation)
                    && !(right instanceof PathPatternPredicate_Instrumentation)
                    && !(right instanceof AndRequestPredicate_Instrumentation)) {
                Util.addPath(request, right.toString());
            }

            return result;
        }

        public Optional<ServerRequest> nest(ServerRequest request) {
            Optional<ServerRequest> result = Weaver.callOriginal();
            if (!Optional.empty().equals(result) && right != null &&
                    !(right instanceof HeadersPredicate_Instrumentation) &&
                    !(right instanceof PathPatternPredicate_Instrumentation)) {
                Util.addPath(request, right.toString());
            }
            return result;
        }
    }

    public static RequestPredicate queryParam(String name, Predicate<String> predicate) {
        final RequestPredicate originalRequestPredicate = Weaver.callOriginal();
        return Util.createRequestPredicate(name, originalRequestPredicate);
    }

    public static RequestPredicate pathExtension(String extension) {
        final RequestPredicate originalRequestPredicate = Weaver.callOriginal();
        return Util.createPathExtensionPredicate(extension, originalRequestPredicate);
    }


    @Weave(originalName = "org.springframework.web.reactive.function.server.RequestPredicates$HeadersPredicate")
    private static class HeadersPredicate_Instrumentation {
    }

    @Weave(originalName = "org.springframework.web.reactive.function.server.RequestPredicates$PathPatternPredicate")
    static class PathPatternPredicate_Instrumentation {
        private PathPattern pattern = Weaver.callOriginal();

        protected RequestPredicates.RequestModifyingPredicate.Result testInternal(ServerRequest request) {
            RequestPredicates.RequestModifyingPredicate.Result result = Weaver.callOriginal();
            boolean matched = result.value();
            if (matched) {
                Util.addPath(request, pattern.getPatternString());
            }
            return result;
        }

        public Optional<ServerRequest> nest(ServerRequest request) {
            Optional<ServerRequest> result = Weaver.callOriginal();
            if (!Optional.empty().equals(result)) {
                Util.addPath(request, pattern.getPatternString());
            }
            return result;
        }
    }
}
