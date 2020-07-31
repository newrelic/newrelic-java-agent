/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;


import com.newrelic.agent.model.SpanCategory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SpanEventsHelper {

    public static Collection<SpanEvent> getSpanEventsByCategory(final SpanCategory category) {
        List<SpanEvent> spanEvents = new LinkedList<>();
        for (SpanEvent event : InstrumentationTestRunner.getIntrospector().getSpanEvents()) {
            if (event.category().equals(category.name())) {
                spanEvents.add(event);
            }
        }
        return spanEvents;
    }

}
