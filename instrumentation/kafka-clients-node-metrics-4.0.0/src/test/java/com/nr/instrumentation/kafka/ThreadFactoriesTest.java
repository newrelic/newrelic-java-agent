/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ThreadFactory;

import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
public class ThreadFactoriesTest {

    @Test
    public void build() {
        ThreadFactory threadFactory = ThreadFactories.build("TestService");
        Thread thread1 = threadFactory.newThread(() -> {});
        Thread thread2 = threadFactory.newThread(() -> {});

        assertThat(thread1.getName(), equalTo("New Relic TestService #1"));
        assertThat(thread2.getName(), equalTo("New Relic TestService #2"));
        assertThat(thread1.isDaemon(), is(true));
        assertThat(thread2.isDaemon(), is(true));
    }
}