/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.helper;


import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import rx.functions.Action0;

import java.util.logging.Level;

/**
 * This action is used in conjunction with {@link com.netflix.hystrix.WeaveAbstractCommand}, and is added to the
 * {@link rx.Observable} representation of a command using {@link rx.Observable#finallyDo(Action0)} so that the
 * command's async token gets expired when the command is finished.
 */
public class ExpireToken implements Action0 {

    private TokenHolder tokenHolder;

    public ExpireToken(TokenHolder tokenHolder) {
        this.tokenHolder = tokenHolder;
    }

    @Override
    public void call() {
        try {
            if (tokenHolder != null && tokenHolder.token != null) {
                tokenHolder.token.expire();
                tokenHolder.token = null;
                tokenHolder = null;
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, t, "An unexpected error occurred while attempting to expire a Hystrix token");
        }
    }

}
