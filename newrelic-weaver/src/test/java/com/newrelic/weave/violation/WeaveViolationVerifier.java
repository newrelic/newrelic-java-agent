/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.violation;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test class iterates over all WeaveViolations and verifies that a corresponding Markdown file with
 * the same name as the WeaveViolation exists in resources/com/newrelic/weave/violation. Each file should
 * contain more information about that violation as well as code examples for documentation purposes.
 */
public class WeaveViolationVerifier {

    @Test
    public void verifyAllMarkdownDescriptionsPresent() throws Exception {
        List<String> missingMarkdownFiles = new ArrayList<>();
        List<String> foundMarkdownFiles = new ArrayList<>();

        File violationResourceDir = null;
        for (WeaveViolationType type : WeaveViolationType.values()) {
            URL violationMarkdown = this.getClass().getResource(type.name() + ".md");
            if (violationMarkdown == null) {
                missingMarkdownFiles.add(type.name() + ".md");
            } else {
                foundMarkdownFiles.add(type.name() + ".md");
                if (violationResourceDir == null) {
                    violationResourceDir = new File(violationMarkdown.getFile()).getParentFile();
                }
            }
        }

        // First check to make sure that all WeaveViolations have a matching Markdown file
        if (!missingMarkdownFiles.isEmpty()) {
            fail("Unable to find the following WeaveViolation markdown files: " + missingMarkdownFiles);
        }

        // Next, check to make sure that we don't have more Markdown files than WeaveViolations
        List<String> allMarkdownFiles = new ArrayList<>(Arrays.asList(violationResourceDir.list()));
        allMarkdownFiles.removeAll(foundMarkdownFiles);
        assertTrue("The following WeaveViolations no longer exist and should be removed: " + allMarkdownFiles,
                allMarkdownFiles.isEmpty());
    }

}
