/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

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
                InputStream inputStream = url.openStream();
                JarInputStream jarStream = new JarInputStream(inputStream);
                
                if (!readToEntry(jarStream, path)) {
                    inputStream.close();
                    throw new IOException("Unable to open stream for " + path + " in " + url.toExternalForm());
                }
                return jarStream;
            }
        }
        
        return url.openStream();
    }
    
    /**
     * Read a jar input stream until a given path is found.
     * 
     * @param jarStream
     * @param path
     * @return  true if the path was found
     * @throws IOException
     */
    private static boolean readToEntry(JarInputStream jarStream, String path) throws IOException {
        for (JarEntry jarEntry = null; (jarEntry = jarStream.getNextJarEntry()) != null;) {
            if (path.equals(jarEntry.getName())) {
                return true;
            }
        }
        return false;
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
