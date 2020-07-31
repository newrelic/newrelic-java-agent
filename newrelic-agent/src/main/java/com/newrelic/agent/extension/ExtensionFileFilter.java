/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.Agent;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;

/**
 * Accepts files with fit the particular input extension.
 */
public class ExtensionFileFilter implements FileFilter {

    /** Picks up all files with this extension. */
    private final String fileExtension;

    /**
     * Creates this XmlExtensionFilter.
     * 
     * @param pFileExt The extension for the files.
     */
    public ExtensionFileFilter(final String pFileExt) {
        super();
        if (pFileExt != null && (pFileExt.length() != 0) && !pFileExt.startsWith(".")) {
            fileExtension = "." + pFileExt;
        } else {
            fileExtension = pFileExt;
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
        if(pFile == null || !pFile.isFile() || !pFile.getName().endsWith(fileExtension)) {
            return false;
        }
        if(!pFile.canRead()){
            Agent.LOG.fine(MessageFormat.format("Unable to read file {0}. Check file permissions", pFile.getAbsolutePath()));
            return false;
        }
        return true;
    }

}
