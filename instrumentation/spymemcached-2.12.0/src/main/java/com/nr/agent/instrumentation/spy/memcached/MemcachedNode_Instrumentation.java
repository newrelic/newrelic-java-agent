/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import net.spy.memcached.ops.Operation;

@Weave(type = MatchType.Interface, originalName = "net.spy.memcached.MemcachedNode")
public class MemcachedNode_Instrumentation {

    public void addOp(Operation op) {
        MemcachedUtil.OPERATION_NODE.set(op.getHandlingNode());
        Weaver.callOriginal();
    }

    public void insertOp(Operation o) {
        MemcachedUtil.OPERATION_NODE.set(o.getHandlingNode());
        Weaver.callOriginal();
    }

}
