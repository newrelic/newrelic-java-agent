/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.batch.item;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.lang.NonNull;

import java.util.List;

@Weave(type = MatchType.Interface, originalName = "org.springframework.batch.item.ItemWriter")
public class ItemWriter_Instrumentation {
    @Trace
    public void write(@NonNull Chunk chunk) throws Exception {
        Weaver.callOriginal();
    }
}
