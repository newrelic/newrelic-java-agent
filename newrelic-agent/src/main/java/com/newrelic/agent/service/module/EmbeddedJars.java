/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class EmbeddedJars {
    private static final Map<String,String> EMBEDDED_FORMAT_TO_EXTENSION = getEmbeddedFormatToExtension("ear","war","jar");
    
    private EmbeddedJars() {}
    
    private static Map<String, String> getEmbeddedFormatToExtension(String... fileExtensions) {
        Builder<String, String> builder = ImmutableMap.builder();
        for (String ext : fileExtensions) {
            builder.put('.' + ext + "!/", ext);
        }
        return builder.build();
    }

    /**
     * Open an input stream for the given url.  If the url points to a jar within a jar, return an input stream starting at the embedded jar.
     * 
     * @param url
     * @throws IOException
     */
    public static InputStream getInputStream(URL url) throws IOException {
        
        for (Entry<String, String> entry : EMBEDDED_FORMAT_TO_EXTENSION.entrySet()) {
            int index = url.toExternalForm().indexOf(entry.getKey());
            if (index > 0) {

                String path = url.toExternalForm().substring(index + entry.getKey().length());
                // add 1 to skip past the `.` and the value length, which is the length of the file extension
                url = new URL(url.toExternalForm().substring(0, index + 1 + entry.getValue().length()));
                // For some reason, some JAR files cannot be read properly by JarInputStream, at least the getNextJarEntry method
                // perhaps related to entry order (https://bugs.openjdk.org/browse/JDK-8031748)
                JarFile jarFile = new JarFile(url.getFile());
                JarEntry innerEntry = jarFile.getJarEntry(path);
                return jarFile.getInputStream(innerEntry);
            }
        }
        
        return url.openStream();
    }

    static JarInputStream getJarInputStream(URL url) throws IOException {
        boolean isEmbedded = isEmbedded(url);
        InputStream stream = getInputStream(url);
        if (!isEmbedded && stream instanceof JarInputStream) {
            return (JarInputStream) stream;
        }
        return new JarInputStream(stream);
    }

    private static boolean isEmbedded(URL url) {
        String externalForm = url.toExternalForm();
        for (String prefix : EMBEDDED_FORMAT_TO_EXTENSION.keySet()) {
            if (externalForm.contains(prefix)) {
                return true;
            }
        }
        
        return false;
    }
}
