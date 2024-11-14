/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.logging;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.api.agent.Transaction;

import java.util.Map;

public class LinkingMetadataHolder {
    private final Map<String, String> linkingMetadata;

    public LinkingMetadataHolder(Map<String, String> linkingMetadata) {
        this.linkingMetadata = linkingMetadata;
    }

    public Map<String, String> getLinkingMetadata() {
        return linkingMetadata;
    }

    public boolean isValid() {
        return  linkingMetadata != null;
    }
}
