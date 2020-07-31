/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.api.agent.weaver.Weave;

@Weave
public abstract class HttpChannel {

    public abstract Request getRequest();
}
