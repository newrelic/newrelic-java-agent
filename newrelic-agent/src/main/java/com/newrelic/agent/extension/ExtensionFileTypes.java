/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.io.FileFilter;

/**
 * Extension file types.
 * 
 * @since Sep 21, 2012
 */
public enum ExtensionFileTypes {

    /**
     * The xml file.
     */
    XML(new ExtensionFileFilter("xml")),
    /**
     * the yml file.
     */
    YML(new MultipleExtensionFileFilter("yml", "yaml")),

    /**
     * Jar files.
     */
    JAR(new ExtensionFileFilter("jar"));

    /**
     * The file filter to use to get the files.
     */
    private FileFilter filter;

    /**
     * 
     * Creates this ExtensionFileTypes.
     * 
     * @param pFilter The filter used to get files.
     */
    private ExtensionFileTypes(final FileFilter pFilter) {
        filter = pFilter;
    }

    /**
     * Gets the field filter.
     * 
     * @return the filter
     */
    public FileFilter getFilter() {
        return filter;
    }

}
