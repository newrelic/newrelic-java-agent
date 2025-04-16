/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.kafka.listener.org.springframework.kafka.listener.adapter;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.kafka.SpringKafkaUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

@Weave(originalName = "org.springframework.kafka.listener.adapter.HandlerAdapter", type = MatchType.ExactClass)
public class HandlerAdapter_Instrumentation {

    private final InvocableHandlerMethod invokerHandlerMethod = Weaver.callOriginal();
    private final DelegatingInvocableHandler_Instrumentation delegatingHandler = Weaver.callOriginal();

    public Object invoke(Message<?> message, Object... providedArgs) {
        InvocableHandlerMethod handlerMethod = null;
        if (invokerHandlerMethod != null) {
            handlerMethod = invokerHandlerMethod;
        } else if (delegatingHandler != null) {
            handlerMethod = delegatingHandler.getHandlerForPayload(message.getPayload().getClass());
        }
        SpringKafkaUtil.nameTransactionFromAnnotation(handlerMethod);
        return Weaver.callOriginal();
    }

}
