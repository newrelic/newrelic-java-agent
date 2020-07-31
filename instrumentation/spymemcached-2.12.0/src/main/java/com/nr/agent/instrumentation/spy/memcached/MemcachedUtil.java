/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import net.spy.memcached.MemcachedNode;

public class MemcachedUtil {

    // This is used to store the node that a given operation executes on for datastore reporting purposes
    public static final ThreadLocal<MemcachedNode> OPERATION_NODE = new ThreadLocal<MemcachedNode>() {
        
    };

}
