package com.newrelic.agent.service.module;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarCollectorHarvestListenerTest {
    @Test
    public void harvestsInCorrectState() {
        JarCollectorService mockService = mock(JarCollectorService.class);
        when(mockService.isEnabled()).thenReturn(true);
        when(mockService.isStartedOrStarting()).thenReturn(true);

        JarCollectorHarvestListener target = new JarCollectorHarvestListener("default", mockService);
        target.beforeHarvest("default", null);

        verify(mockService, times(1)).harvest();
    }

    @Test
    public void doesNotHarvestOnOtherAppName() {
        JarCollectorService mockService = mock(JarCollectorService.class);
        when(mockService.isEnabled()).thenReturn(true);
        when(mockService.isStartedOrStarting()).thenReturn(true);

        JarCollectorHarvestListener target = new JarCollectorHarvestListener("default", mockService);
        target.beforeHarvest("other", null);

        verify(mockService, never()).harvest();
    }

    @Test
    public void doesNotHarvestIfNotEnabled() {
        JarCollectorService mockService = mock(JarCollectorService.class);
        when(mockService.isEnabled()).thenReturn(false);
        when(mockService.isStartedOrStarting()).thenReturn(true);

        JarCollectorHarvestListener target = new JarCollectorHarvestListener("default", mockService);
        target.beforeHarvest("default", null);

        verify(mockService, never()).harvest();
    }

    @Test
    public void doesNotHarvestIfNotStarted() {
        JarCollectorService mockService = mock(JarCollectorService.class);
        when(mockService.isEnabled()).thenReturn(true);
        when(mockService.isStartedOrStarting()).thenReturn(false);

        JarCollectorHarvestListener target = new JarCollectorHarvestListener("default", mockService);
        target.beforeHarvest("default", null);

        verify(mockService, never()).harvest();
    }
}