/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.threads;

import java.lang.management.ThreadInfo;

public class BasicThreadInfo {
    private final long id;
    private final String name;
    
    public BasicThreadInfo(Thread thread) {
        this(thread.getId(), thread.getName());
    }
    
    public BasicThreadInfo(ThreadInfo thread) {
        this(thread.getThreadId(), thread.getThreadName());
    }
    
    public BasicThreadInfo(long id, String name) {
        super();
        this.id = id;
        this.name = name;
    }
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

}
