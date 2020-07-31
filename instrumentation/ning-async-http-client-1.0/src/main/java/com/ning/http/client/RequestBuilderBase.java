/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ning.http.client;

import com.newrelic.api.agent.weaver.Weave;

/**
 *  Instrumentation for RequestBuilderBase.RequestImpl
 */
@Weave
abstract class RequestBuilderBase {

    @Weave
    private abstract static class RequestImpl {
        private Headers headers;

        public Headers getHeaders() {
            // The implementation returns an unmodifiable view of the headers.
            // CAT requires that the Agent add headers.  Return the unwrapped headers instead.
            return headers;
        }
    }
}
