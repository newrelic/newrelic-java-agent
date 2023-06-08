package com.newrelic.agent.logging;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;

import java.util.logging.Level;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class FineFilterTest {

    @Test
    public void getFineFilter_returnsInstance() {
        assertNotNull(FineFilter.getFineFilter());
    }

    @Test
    public void getLevel_returnsCurrentLevel() {
        FineFilter ff = FineFilter.getFineFilter();
        ff.setLevel(Level.ALL);
        assertEquals(Level.ALL, ff.getLevel());
    }

    @Test
    public void filter_whenNotStarted_returnsNeutral() {
        LogEvent logEvent = mock(LogEvent.class);
        assertEquals(Filter.Result.NEUTRAL, FineFilter.getFineFilter().filter(logEvent));
    }

    @Test
    public void filter_whenStartedAndLevelIsInfoAndMarkerNotSet_returnsNeutral() {
        LogEvent logEvent = mock(LogEvent.class);
        FineFilter ff = FineFilter.getFineFilter();
        when(logEvent.getMarker()).thenReturn(null);

        ff.start();
        assertEquals(Filter.Result.NEUTRAL, ff.filter(logEvent));
    }

    @Test
    public void filter_whenStartedAndLevelIsFineAndMarkerIsMatch_returnsAccept() {
        LogEvent logEvent = mock(LogEvent.class);
        FineFilter ff = FineFilter.getFineFilter();
        ff.setLevel(Level.FINE);
        when(logEvent.getMarker()).thenReturn(Log4jMarkers.FINE_MARKER);

        ff.start();
        assertEquals(Filter.Result.ACCEPT, ff.filter(logEvent));
    }

    @Test
    public void filter_whenStartedAndLevelIsFineAndMarkerIsFail_returnsAccept() {
        LogEvent logEvent = mock(LogEvent.class);
        FineFilter ff = FineFilter.getFineFilter();
        ff.setLevel(Level.FINE);
        when(logEvent.getMarker()).thenReturn(Log4jMarkers.FINER_MARKER);

        ff.start();
        assertEquals(Filter.Result.DENY, ff.filter(logEvent));
    }

    @Test
    public void isEnabledFor_whenFilterIsConfiguredWithFinest_returnsTrue() {
        FineFilter ff = FineFilter.getFineFilter();
        ff.setLevel(Level.FINEST);
        assertTrue(ff.isEnabledFor(Level.FINEST));
        assertTrue(ff.isEnabledFor(Level.FINER));
        assertTrue(ff.isEnabledFor(Level.FINE));
        assertTrue(ff.isEnabledFor(Level.INFO));
        assertTrue(ff.isEnabledFor(Level.CONFIG));
        assertTrue(ff.isEnabledFor(Level.WARNING));
        assertTrue(ff.isEnabledFor(Level.SEVERE));
    }

    @Test
    public void isEnabledFor_whenFilterIsConfiguredWithAll_returnsTrue() {
        FineFilter ff = FineFilter.getFineFilter();
        ff.setLevel(Level.ALL);
        assertTrue(ff.isEnabledFor(Level.FINEST));
        assertTrue(ff.isEnabledFor(Level.FINER));
        assertTrue(ff.isEnabledFor(Level.FINE));
        assertTrue(ff.isEnabledFor(Level.INFO));
        assertTrue(ff.isEnabledFor(Level.CONFIG));
        assertTrue(ff.isEnabledFor(Level.WARNING));
        assertTrue(ff.isEnabledFor(Level.SEVERE));
    }

    @Test
    public void isEnabledFor_whenFilterIsConfiguredWithOff_returnsFalse() {
        FineFilter ff = FineFilter.getFineFilter();
        ff.setLevel(Level.OFF);
        assertFalse(ff.isEnabledFor(Level.FINEST));
        assertFalse(ff.isEnabledFor(Level.FINER));
        assertFalse(ff.isEnabledFor(Level.FINE));
        assertFalse(ff.isEnabledFor(Level.INFO));
        assertFalse(ff.isEnabledFor(Level.CONFIG));
        assertFalse(ff.isEnabledFor(Level.WARNING));
        assertFalse(ff.isEnabledFor(Level.SEVERE));
    }
}
