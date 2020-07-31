/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.helper;

import com.newrelic.agent.bridge.AgentBridge;
import rx.functions.Action0;

import java.util.logging.Level;

/**
 * An action that will be executed when an Observable is subscribed to (executed) that creates a token and stores
 * it in the holder for other code to link/expire.
 */
public class GetToken implements Action0 {

    private TokenHolder tokenHolder;

    public GetToken(TokenHolder tokenHolder) {
        this.tokenHolder = tokenHolder;
    }

    @Override
    public void call() {
        try {
            tokenHolder.token = AgentBridge.getAgent().getTransaction().getToken();
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, t, "An unexpected error occurred while attempting to get a Hystrix token");
        }
    }

}
