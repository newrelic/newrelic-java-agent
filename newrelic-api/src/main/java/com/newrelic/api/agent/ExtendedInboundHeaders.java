/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Collections;
import java.util.List;

public abstract class ExtendedInboundHeaders implements InboundHeaders {

    public List<String> getHeaders(String name) {
        return Collections.emptyList();
    }

}
