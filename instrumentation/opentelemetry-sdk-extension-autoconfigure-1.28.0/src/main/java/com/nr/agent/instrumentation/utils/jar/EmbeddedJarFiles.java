/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.jar;

import java.io.File;
import java.io.IOException;

public interface EmbeddedJarFiles {

    /**
     * Returns the names of the api jars that are embedded in the agent without their extension.
     */
    String[] getEmbeddedAgentJarFileNames();

    /**
     * Returns a jar file created by reading the contents of a jar embedded inside of the agent jar and writing it out
     * to a temp file. The result is cached so that multiple calls to this api for a given jar name reuse the same temp
     * file.
     */
    File getJarFileInAgent(String jarNameWithoutExtension) throws IOException;

}
