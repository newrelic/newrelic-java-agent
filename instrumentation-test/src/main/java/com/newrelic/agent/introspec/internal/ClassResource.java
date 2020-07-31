/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.weave.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ClassResource {
    public static List<ClassResource> fromClassLoader(URLClassLoader classLoader) throws IOException {
        List<ClassResource> result = new ArrayList<>();
        URL[] urls = classLoader.getURLs();
        for (URL url : urls) {
            fromURL(url, result);
        }
        return result;
    }

    public static void fromURL(URL url, Collection<ClassResource> result) throws IOException {
        try {
            URI uri = url.toURI();
            File file = new File(uri);
            if (file.isDirectory()) {
                fromDirectory(url, uri, file, result);
            } else if (url.toString().endsWith(".jar")) {
                JarFile jarFile = new JarFile(file);
                fromJAR(url, jarFile, result);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static void fromJAR(URL sourceURL, JarFile jarFile, Collection<ClassResource> result) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                result.add(new ClassResource(entry.getName(), sourceURL));
            }
        }
    }

    private static void fromDirectory(URL sourceURL, URI sourceURI, File dir, Collection<ClassResource> result) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    fromDirectory(sourceURL, sourceURI, child, result);
                } else if (child.getName().endsWith(".class")) {
                    String resourceName = sourceURI.relativize(child.toURI()).getPath();
                    result.add(new ClassResource(resourceName, sourceURL));
                }
            }
        }
    }

    public final String resourceName;
    public final URL sourceURL;
    private URL resourceURL;
    private byte[] resourceBytes;

    public ClassResource(String resourceName, URL sourceURL) {
        this.resourceName = resourceName;
        this.sourceURL = sourceURL;
    }

    public byte[] read() throws IOException {
        if (resourceBytes == null) {
            resourceBytes = Streams.read(getResourceURL().openConnection().getInputStream(), true);
        }
        return resourceBytes;
    }

    public void setResourceBytes(byte[] resourceBytes) {
        this.resourceBytes = resourceBytes;
    }

    public URL getResourceURL() throws MalformedURLException {
        if (resourceURL == null) {
            String source = sourceURL.toString();
            if (source.endsWith(".jar")) {
                resourceURL = new URL("jar:" + source + "!/" + resourceName);
            } else if (sourceURL.getProtocol().equals("memory")) {
                resourceURL = sourceURL;
            } else {
                resourceURL = new URL(source + "/" + resourceName);
            }
        }
        return resourceURL;
    }
}
