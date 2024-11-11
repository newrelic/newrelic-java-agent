/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.language;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWork;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SourceLibraryDetectorTest {

    private SourceLibraryDetector sourceLibraryDetector;

    @BeforeClass
    public static void setupClass() {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        try {
            serviceManager.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() throws Exception {
        sourceLibraryDetector = new SourceLibraryDetector();
    }

    @Test
    public void testSamplerRun() throws Exception {
        sourceLibraryDetector.run();
        ArgumentCaptor<StatsWork> captor = ArgumentCaptor.forClass(StatsWork.class);
        verify(ServiceFactory.getStatsService(), times(2)).doStatsWork(captor.capture(), anyString());

        StatsWork statsWork = captor.getValue();

        // since the RecordMetric class is package private, we can't actually validate the values contained within it
        assertNotNull(statsWork);
    }

    @Test
    public void unexpectedExceptionIsCaughtWhenDetectingLangauges() throws Exception {
        sourceLibraryDetector = spy(sourceLibraryDetector);
        doThrow(RuntimeException.class).when(sourceLibraryDetector).detectSourceLanguages();
        try {
            sourceLibraryDetector.run();
        } catch (Throwable throwable) {
            fail("Exception should not have made it all the way to the test");
        }
    }
}
