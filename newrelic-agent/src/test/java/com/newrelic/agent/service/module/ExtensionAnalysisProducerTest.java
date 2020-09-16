package com.newrelic.agent.service.module;

import com.newrelic.api.agent.Logger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtensionAnalysisProducerTest {
    @Test
    public void submitsAllFiles() {
        JarAnalystFactory jarAnalystFactory = mock(JarAnalystFactory.class);
        when(jarAnalystFactory.createWeavePackageAnalyzer(any(File.class))).thenReturn(mock(Runnable.class));

        ExecutorService executorService = mock(ExecutorService.class);
        ExtensionAnalysisProducer target = new ExtensionAnalysisProducer(jarAnalystFactory, executorService, mock(Logger.class));

        Set<File> expectedFiles = new HashSet<>(Arrays.asList(new File("file1"), new File("file2"), new File("file3")));
        target.loaded(expectedFiles);

        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(jarAnalystFactory, times(3)).createWeavePackageAnalyzer(fileArgumentCaptor.capture());
        Set<File> capturedFiles = new HashSet<>(fileArgumentCaptor.getAllValues());
        assertEquals(expectedFiles, capturedFiles);

        verify(executorService, times(3)).submit(any(Runnable.class));
    }

}