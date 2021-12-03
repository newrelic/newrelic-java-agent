/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws;

import play.api.libs.ws.StandaloneWSResponse;

public interface ResponseRunnable {

    void onResponse(StandaloneWSResponse standaloneWSResponse);

}