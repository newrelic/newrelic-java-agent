/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.kafka.listener.org.springframework.kafka.listener.adapter;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

@Weave(originalName = "org.springframework.kafka.listener.adapter.DelegatingInvocableHandler", type = MatchType.ExactClass)
public abstract class DelegatingInvocableHandler_Instrumentation {
    protected abstract InvocableHandlerMethod getHandlerForPayload(Class<?> payloadClass);
}
