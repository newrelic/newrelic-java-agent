/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.logging.log4j.core.layout;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.nio.charset.Charset;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;

@Weave(originalName = "org.apache.logging.log4j.core.layout.AbstractStringLayout", type = MatchType.ExactClass)
public abstract class AbstractStringLayout_Instrumentation {

    private final Charset charset = Weaver.callOriginal();

    protected byte[] getBytes(final String s) {
        String modified = s;
        // It is possible that the log being formatted into JSON might already have NR-LINKING metadata from JUL instrumentation
        if (!s.contains("NR-LINKING")) {
            int indexToInsertNrLinkingMetadata = s.indexOf("\n", s.indexOf("message")) - 1;
            // Replace the JSON string with modified version that includes NR-LINKING metadata
            modified = new StringBuilder(s).insert(indexToInsertNrLinkingMetadata, getLinkingMetadataBlob()).toString();
        }
        return modified.getBytes(charset);
    }

}
