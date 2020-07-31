/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.IOException;

/**
 * Finds {@link ClassInformation} for classes.
 */
public interface ClassInformationFinder {

    /**
     * Find the {@link ClassInformation} for the specified internal class name.
     * 
     * @param internalName internal class name
     * @return the {@link ClassInformation} for the specified internal class name
     * @throws IOException
     */
    ClassInformation getClassInformation(String internalName) throws IOException;
}
