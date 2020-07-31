/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.objectweb.asm.Type;

/**
 * Utility class for reading {@link InputStream}s and copying them to {@link OutputStream}s.
 */
public final class Streams {
    /**
     * Default buffer size to use when copying.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 8;

    private Streams() {
    }

    /**
     * Return a byte[] of the passed in Class object. This will load the bytes from the ClassLoader that loaded the Class.
     * 
     * @param clazz the class to get the byte[] for
     * @return bytes of the passed in Class
     */
    public static byte[] getClassBytes(Class clazz) throws IOException {
        return getClassBytes(clazz.getClassLoader(), Type.getInternalName(clazz));
    }

    /**
     * Return a byte[] of the passed in class name using the provided ClassLoader to load the bytes.
     * 
     * @param classLoader the ClassLoader to use to load the bytes
     * @param name the name of the Class to use
     * @return bytes of the passed in class name loaded from the provided ClassLoader
     */
    public static byte[] getClassBytes(ClassLoader classLoader, String name) throws IOException {
        name = name.replaceAll("\\.", "/") + ".class";
        InputStream iStream = null;
        if (classLoader == null) {
            URL resource = BootstrapLoader.get().findResource(name);
            if (resource != null) {
                iStream = resource.openStream();
            }
        } else {
            iStream = classLoader.getResourceAsStream(name);
        }

        if (iStream != null) {
            try {
                ByteArrayOutputStream oStream = new ByteArrayOutputStream();

                Streams.copy(iStream, oStream);
                return oStream.toByteArray();
            } finally {
                iStream.close();
            }
        }
        return null;
    }

    /**
     * Copy the input stream to the output stream without closing either of them.
     *
     * @param input stream to read from
     * @param output stream to write to
     * @return number of bytes copied
     * @throws IOException
     */
    public static int copy(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE, false);
    }

    /**
     * Copy the input stream to the output stream.
     *
     * @param input stream to read from
     * @param output stream to write to
     * @param closeStreams whether to close the streams after the copy operation is complete
     * @return number of bytes copied
     * @throws IOException
     */
    public static int copy(InputStream input, OutputStream output, boolean closeStreams) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE, closeStreams);
    }

    /**
     * Copy the input stream to the output stream.
     * @param input stream to read from
     * @param output stream to write to
     * @param bufferSize size of internal buffer to use when copying
     * @return number of bytes copied
     * @throws IOException
     */
    public static int copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
        return copy(input, output, bufferSize, false);
    }

    /**
     * Copy the input stream to the output stream.
     *
     * @param input stream to read from
     * @param output stream to write to
     * @param bufferSize size of internal buffer to use when copying
     * @param closeStreams whether to close the streams after the copy operation is complete
     * @return number of bytes copied
     * @throws IOException
     */
    public static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams)
            throws IOException {
        try {
            if (0 == bufferSize) {
                return 0;
            }
            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } finally {
            if (closeStreams) {
                input.close();
                output.close();
            }
        }
    }

    /**
     * Read the available bytes from the {@link InputStream} into a byte[].
     * 
     * @param input stream to read from
     * @param closeInputStream whether to close the stream after the read operation is complete
     * @return byte[] containing the available bytes from the {@link InputStream}
     * @throws IOException
     */
    public static byte[] read(InputStream input, boolean closeInputStream) throws IOException {
        return read(input, input.available(), closeInputStream);
    }

    /**
     * Read the expected number of bytes from the {@link InputStream} into a byte[].
     * 
     * @param input stream to read from
     * @param expectedSize expected number of bytes to read from the {@link InputStream}
     * @param closeInputStream whether to close the stream after the read operation is complete
     * @return byte[] containing the expected number of bytes from the {@link InputStream}
     * @throws IOException
     */
    public static byte[] read(InputStream input, int expectedSize, boolean closeInputStream) throws IOException {
        if (expectedSize <= 0) {
            expectedSize = DEFAULT_BUFFER_SIZE;
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(expectedSize);
        copy(input, outStream, expectedSize, closeInputStream);
        return outStream.toByteArray();
    }
}
