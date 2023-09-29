package com.newrelic.agent.service.module;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.api.agent.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarCollectorServiceImplTest {

    @Mock
    public IRPMService rpmService;

    @Captor
    public ArgumentCaptor<List<JarData>> listArgumentCaptor;

    private final JarData initialJarAlreadySent = new JarData("jar1", new JarInfo("v1", Collections.<String, String>emptyMap()));
    private final JarData expectedSentJar = new JarData("jar2", new JarInfo("v2", Collections.<String, String>emptyMap()));

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        RPMServiceManager rpmServiceManager = mock(RPMServiceManager.class);
        when(rpmServiceManager.getOrCreateRPMService(anyString())).thenReturn(rpmService);

        ServiceManager mockServiceManager = mock(ServiceManager.class);
        when(mockServiceManager.getRPMServiceManager()).thenReturn(rpmServiceManager);
        ServiceFactory.setServiceManager(mockServiceManager);
    }

    @After
    public void after() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void harvestsJarsThatHaventBeenSent() throws Exception {
        TrackedAddSet<JarData> set = new TrackedAddSet<>();
        set.accept(initialJarAlreadySent);
        set.resetReturningAll();
        set.accept(expectedSentJar);

        AtomicBoolean shouldSendAllJars = new AtomicBoolean(false);

        JarCollectorService target = getTarget(set, shouldSendAllJars);
        target.harvest("any name");

        verifyTriedToSendExpectedJar();
    }

    @Test
    public void resendsErrorJarsWithDelta() throws Exception {
        TrackedAddSet<JarData> set = new TrackedAddSet<>();
        set.accept(initialJarAlreadySent);
        set.resetReturningAll();
        set.accept(expectedSentJar);

        AtomicBoolean shouldSendAllJars = new AtomicBoolean(false);

        JarCollectorService target = getTarget(set, shouldSendAllJars);

        // make harvest fail
        doThrow(new Exception("~~ oops ~~")).when(rpmService).sendModules(ArgumentMatchers.<List<JarData>>any());
        target.harvest("any name");
        verifyTriedToSendExpectedJar();
        reset(rpmService);

        // this is another delta
        JarData postErrorAdd = new JarData("jar3", new JarInfo("v3", Collections.<String, String>emptyMap()));
        set.accept(postErrorAdd);
        target.harvest("any name");

        verify(rpmService, times(1)).sendModules(listArgumentCaptor.capture());
        assertEquals(ImmutableSet.of(expectedSentJar, postErrorAdd), new HashSet<>(listArgumentCaptor.getValue()));
    }

    @Test
    public void resendsErrorJarsEvenWithoutDelta() throws Exception {
        TrackedAddSet<JarData> set = new TrackedAddSet<>();
        set.accept(initialJarAlreadySent);
        set.resetReturningAll();
        set.accept(expectedSentJar);

        AtomicBoolean shouldSendAllJars = new AtomicBoolean(false);

        JarCollectorService target = getTarget(set, shouldSendAllJars);

        // make harvest fail
        doThrow(new Exception("~~ oops ~~")).when(rpmService).sendModules(ArgumentMatchers.<List<JarData>>any());
        target.harvest("any name");
        verifyTriedToSendExpectedJar();

        // allow second harvest to succeed
        reset(rpmService);

        target.harvest("any name");
        verifyTriedToSendExpectedJar();
    }

    @Test
    public void harvestsAllJarsOnReset() throws Exception {
        TrackedAddSet<JarData> set = new TrackedAddSet<>();
        set.accept(initialJarAlreadySent);
        set.resetReturningAll();
        set.accept(expectedSentJar);

        AtomicBoolean shouldSendAllJars = new AtomicBoolean(false);

        JarCollectorService target = getTarget(set, shouldSendAllJars);

        // harvest the delta, only jar2
        target.harvest("any name");
        verifyTriedToSendExpectedJar();
        reset(rpmService);

        // send all jars next time!
        shouldSendAllJars.set(true);
        target.harvest("any name");

        verify(rpmService, times(1)).sendModules(listArgumentCaptor.capture());
        assertEquals(ImmutableSet.of(expectedSentJar, initialJarAlreadySent), new HashSet<>(listArgumentCaptor.getValue()));
    }

    @Test
    public void resetsSendAllFlagAfterSendingAll() throws Exception {
        TrackedAddSet<JarData> set = new TrackedAddSet<>();
        set.accept(initialJarAlreadySent);
        set.resetReturningAll();
        set.accept(expectedSentJar);

        AtomicBoolean shouldSendAllJars = new AtomicBoolean(true);

        JarCollectorService target = getTarget(set, shouldSendAllJars);
        target.harvest("any name");
        verify(rpmService, times(1)).sendModules(listArgumentCaptor.capture());
        assertEquals(ImmutableSet.of(expectedSentJar, initialJarAlreadySent), new HashSet<>(listArgumentCaptor.getValue()));
        reset(rpmService);

        assertFalse(shouldSendAllJars.get());
    }

    private void verifyTriedToSendExpectedJar() throws Exception {
        verify(rpmService, times(1)).sendModules(Collections.singletonList(expectedSentJar));
    }

    private JarCollectorServiceImpl getTarget(
            TrackedAddSet<JarData> set,
            AtomicBoolean shouldSendAllJars) {
        return new JarCollectorServiceImpl(
                mock(Logger.class),
                true,
                shouldSendAllJars,
                set,
                mock(ClassToJarPathSubmitter.class)
        );
    }

}