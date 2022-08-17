/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

import com.newrelic.api.agent.Trace;

public class DefaultUserFeaturesImpl implements UserFeaturesResource {

    @Override
    @Trace(dispatcher = true)
    public String getUserFeatures() {
        return "User Features!";
    }

}
