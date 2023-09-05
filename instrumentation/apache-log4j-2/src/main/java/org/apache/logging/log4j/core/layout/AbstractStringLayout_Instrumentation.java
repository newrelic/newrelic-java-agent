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
import org.apache.logging.log4j.core.util.StringEncoder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.BLOB_PREFIX;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;

@Weave(originalName = "org.apache.logging.log4j.core.layout.AbstractStringLayout", type = MatchType.ExactClass)
public abstract class AbstractStringLayout_Instrumentation {

    private transient Charset charset = Weaver.callOriginal();
    private final String charsetName = Weaver.callOriginal();
    private final boolean useCustomEncoding = Weaver.callOriginal();

    protected byte[] getBytes(final String s) {
        String modified = s;
        // It is possible that the log being formatted into JSON might already have NR-LINKING metadata from JUL instrumentation
        if (!s.contains(BLOB_PREFIX)) {
            int indexToInsertNrLinkingMetadata = s.indexOf("\n", s.indexOf("message")) - 1;
            // Replace the JSON string with modified version that includes NR-LINKING metadata
            modified = new StringBuilder(s).insert(indexToInsertNrLinkingMetadata, getLinkingMetadataBlob()).toString();
        }
        if (useCustomEncoding) {
            return StringEncoder.encodeSingleByteChars(modified);
        }
        try {
            return modified.getBytes(charsetName);
        } catch (final UnsupportedEncodingException e) {
            return modified.getBytes(charset);
        }
    }

}
