/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import java.io.File;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

public class DataFetcherTest {
    private static final Pattern MEMORY_PATTERN = Pattern.compile("memory: ([0-9]+ kb)");

    @Test
    public void testParseLongRam() {
        Assert.assertEquals(0, DataFetcher.parseLongRam(null));
        Assert.assertEquals(123, DataFetcher.parseLongRam("123"));
        Assert.assertEquals(0, DataFetcher.parseLongRam("123IAmNotANumber"));
    }

    @Test
    public void testReadFromInvalidFile() {
        Assert.assertNull(DataFetcher.findLastMatchInFile(new File(".fasdThisIsNot/\\AValidFILE8)88"), MEMORY_PATTERN));
    }
}
