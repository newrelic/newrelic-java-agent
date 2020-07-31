/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import org.junit.Test;

import static com.newrelic.agent.model.SpanCategory.generic;
import static org.junit.Assert.assertSame;

public class SpanCategoryTest {

    @Test
    public void testFromString() {
        for (SpanCategory spanCategory : SpanCategory.values()) {
            SpanCategory result = SpanCategory.fromString(spanCategory.toString());
            assertSame(result, spanCategory);
        }
        assertSame(generic, SpanCategory.fromString("blargustarg"));
    }

}