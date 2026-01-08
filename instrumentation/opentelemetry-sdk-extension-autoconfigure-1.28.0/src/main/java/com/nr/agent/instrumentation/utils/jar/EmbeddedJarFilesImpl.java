/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {
    /**
     * The tempdir setting is not prefixed with `config` because it can't be set through the config file,
     * only as a system property.
     */
    private static final String NEWRELIC_TEMPDIR = "newrelic.tempdir";

    public static final String AGENT_BRIDGE_JAR_NAME = "agent-bridge";

    public static final String API_JAR_NAME = "newrelic-api";

    public static final String WEAVER_API_JAR_NAME = "newrelic-weaver-api";

    public static final String NEWRELIC_SECURITY_AGENT = "newrelic-security-agent";

    private static final String[] INTERNAL_JAR_FILE_NAMES = new String[] { AGENT_BRIDGE_JAR_NAME,
            API_JAR_NAME, WEAVER_API_JAR_NAME, NEWRELIC_SECURITY_AGENT };

    public static final EmbeddedJarFiles INSTANCE = new EmbeddedJarFilesImpl();

    public File load(String jarNameWithoutExtension) throws IOException {
        InputStream jarStream = EmbeddedJarFilesImpl.class.getClassLoader().getResourceAsStream(
                jarNameWithoutExtension + ".jar");
        if (jarStream == null) {
            throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
        }

        File file = File.createTempFile(jarNameWithoutExtension, ".jar", getTempDir());
        file.deleteOnExit(); // Doesn't need to be kept after shutdown.

        try (OutputStream out = new FileOutputStream(file)) {
            copy(jarStream, out, 8096, true);
            return file;
        }
    }

    private final String[] jarFileNames;

    public EmbeddedJarFilesImpl() {
        this(INTERNAL_JAR_FILE_NAMES);
    }

    public EmbeddedJarFilesImpl(String[] jarFileNames) {
        super();
        this.jarFileNames = jarFileNames;
    }

    @Override
    public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
        return load(jarNameWithoutExtension);
    }

    @Override
    public String[] getEmbeddedAgentJarFileNames() {
        return jarFileNames;
    }

    /**
     * Copy bytes from an InputStream to an OutputStream.
     *
     * @param input  the InputStream to read from
     * @param output the OutputStream to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(InputStream input, OutputStream output, int bufferSize, boolean closeStreams) throws IOException {
        try {
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
     * Returns the tempdir that the agent should use, or null if the default temp directory should
     * be used. This can be set using the newrelic.tempdir system property.
     */
    public static File getTempDir() {
        String tempDir = System.getProperty(NEWRELIC_TEMPDIR);
        if (null != tempDir) {
            File tempDirFile = new File(tempDir);
            if (tempDirFile.exists()) {
                return tempDirFile;
            } else {
                System.err.println("Temp directory specified by newrelic.tempdir does not exist - " + tempDir);
            }
        }
        return null;
    }
}
