/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.client;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.RestTemplateUtils;
import org.springframework.http.HttpMethod;

import java.net.URI;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.web.client.RestTemplate")
public abstract class RestTemplate_Instrumentation {

    /**
     * 4-parameter version - exists in Spring 3.x through 6.x
     * In Spring 6.x, this method delegates to the 5-parameter version.
     * We instrument both to ensure complete coverage.
     */
    @Trace(leaf = true)
    protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback,
                              ResponseExtractor<T> responseExtractor) throws RestClientException {

        T result;
        try {
            result = Weaver.callOriginal();
        } catch (Exception e) {
            RestTemplateUtils.handleUnknownHost(e);
            throw e;
        }

        RestTemplateUtils.processResponse(url, method, result);
        return result;
    }

    /**
     * 5-parameter version - added in Spring 6.0
     * Spring 6.x directly calls this method for most operations (does NOT always delegate through 4-parameter).
     * This is the primary execution path in Spring 6.x.
     * Removed in Spring 7.0 (method signature changes).
     */
    @Trace(leaf = true)
    protected <T> T doExecute(URI url, String uriTemplate, HttpMethod method,
                              RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {

        T result;
        try {
            result = Weaver.callOriginal();
        } catch (Exception e) {
            RestTemplateUtils.handleUnknownHost(e);
            throw e;
        }

        RestTemplateUtils.processResponse(url, method, result);
        return result;
    }
}