/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.tracers.Tracer;

public class TransactionActivityTree extends BaseTree<TraceProfileSegment> {
    
    private final AtomicLong cpuTimeInNanos = new AtomicLong();

    public TransactionActivityTree(IProfile profile) {
        super(profile);
    }

    @Override
    protected Map<String, Object> getExtraData() {
        long cpuTime = cpuTimeInNanos.get();
        if (cpuTime > 0) {
            return ImmutableMap.<String, Object>of("cpu_time", TimeUnit.NANOSECONDS.toMillis(cpuTime));
        } else {
            return ImmutableMap.of();
        }
    }

    public void add(TransactionActivity activity, Map<Tracer, Collection<Tracer>> tracerTree) {
        add(null, activity.getRootTracer(), tracerTree);
        
        cpuTimeInNanos.addAndGet(activity.getTotalCpuTime());
    }

    private void add(TraceProfileSegment parent, Tracer tracer, Map<Tracer, Collection<Tracer>> children) {
        ProfiledMethod profiledMethod = profile.getProfiledMethodFactory().getProfiledMethod(tracer);
        TraceProfileSegment thisSegment = add(profiledMethod, parent, true);
        thisSegment.update(tracer);

        Collection<Tracer> kids = children.get(tracer);
        if (null != kids) {
            for (Tracer kid : kids) {
                add(thisSegment, kid, children);
            }
        }
    }

    @Override
    protected TraceProfileSegment createProfiledMethod(ProfiledMethod method) {
        return TraceProfileSegment.newProfileSegment(method);
    }

}
