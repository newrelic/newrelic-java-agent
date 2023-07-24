/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Type;

import com.newrelic.weave.utils.Streams;

public class ClassResolversTest {

    @Test
    public void classloaderMissing() throws IOException {
        ClassResolver resolver = ClassResolvers.getClassLoaderResolver(ClassLoader.getSystemClassLoader());
        Assert.assertNull(resolver.getClassResource("foo/bar"));
    }

    @Test
    public void classloaderFound() throws IOException {
        ClassResolver resolver = ClassResolvers.getClassLoaderResolver(ClassLoader.getSystemClassLoader());
        Assert.assertNotNull(resolver.getClassResource(Type.getInternalName(List.class)));
    }

    @Test
    public void jarFound() throws IOException {
        ClassResolver resolver = ClassResolvers.getJarClassResolver(createJarFile());
        InputStream resource = resolver.getClassResource("com/newrelic/Test");
        Assert.assertNotNull(resource);

        byte[] bytes = Streams.read(resource, true);
        Assert.assertEquals(4, bytes.length);
    }

    @Test
    public void multi() throws IOException {
        ClassResolver resolver1 = Mockito.mock(ClassResolver.class);
        ClassResolver resolver2 = Mockito.mock(ClassResolver.class);

        Mockito.when(resolver1.getClassResource("foo/bar")).thenReturn(new ByteArrayInputStream(new byte[0]));

        ClassResolver multi = ClassResolvers.getMultiResolver(resolver1, resolver2);
        Assert.assertNotNull(multi.getClassResource("foo/bar"));

        Mockito.verify(resolver1, Mockito.times(1)).getClassResource("foo/bar");

        Mockito.verifyNoMoreInteractions(resolver1);
        Mockito.verifyNoInteractions(resolver2);
    }

    private File createJarFile() throws IOException {
        File file = File.createTempFile("test", ".jar");
        file.deleteOnExit();

        JarOutputStream jar = new JarOutputStream(new FileOutputStream(file));

        JarEntry entry = new JarEntry("com/newrelic/Test.class");
        jar.putNextEntry(entry);

        jar.write(new byte[] { 0xB, 0xE, 0xE, 0xF });

        jar.closeEntry();

        jar.close();

        return file;
    }
}
