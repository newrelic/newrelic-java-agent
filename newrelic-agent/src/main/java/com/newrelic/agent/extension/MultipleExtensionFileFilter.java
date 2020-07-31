/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts files which match one of the file extensions.
 * 
 * @since Sep 21, 2012
 */
public class MultipleExtensionFileFilter implements FileFilter {

    /** The list of valid extensions. */
    private final List<String> extensions;

    /**
     * 
     * Creates this MultipleExtensionFileFilter.
     * 
     * @param pFileExtn Valid file extensions.
     */
    public MultipleExtensionFileFilter(String... pFileExtn) {
        extensions = new ArrayList<>();
        for (String ext : pFileExtn) {
            if (ext != null && (ext.length() != 0) && !ext.startsWith(".")) {
                extensions.add("." + ext);
            } else {
                extensions.add(ext);
            }
        }
    }

    /**
     * Accepts files which are a file, are readable, and ends with the input file extension.
     * 
     * @param pFile The file to be checked.
     * @return True if the file should be accepted.
     */
    @Override
    public boolean accept(final File pFile) {
        if (pFile != null && pFile.isFile() && pFile.canRead()) {
            String name = pFile.getName();
            for (String ext : extensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

}
