package com.nr.instrumentation.kafka;

import org.junit.Test;

import java.util.concurrent.ThreadFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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