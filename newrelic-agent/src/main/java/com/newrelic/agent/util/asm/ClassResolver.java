/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.io.IOException;
import java.io.InputStream;

public interface ClassResolver {
    /**
     * Returns an input stream for the given internal class name, or null if it can't be resolved.
     * 
     * @param internalClassName an internal class name (slash delimited)
     * @throws IOException
     */
    InputStream getClassResource(String internalClassName) throws IOException;
}
