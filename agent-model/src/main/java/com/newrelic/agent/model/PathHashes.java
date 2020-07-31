/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public class PathHashes {

    private final Integer pathHash;
    private final Integer referringPathHash;
    private final String alternatePathHashes;

    public PathHashes(Integer pathHash, Integer referringPathHash, String alternatePathHashes) {
        this.pathHash = pathHash;
        this.referringPathHash = referringPathHash;
        this.alternatePathHashes = alternatePathHashes;
    }

    public Integer getPathHash() {
        return pathHash;
    }

    public Integer getReferringPathHash() {
        return referringPathHash;
    }

    public String getAlternatePathHashes() {
        return alternatePathHashes;
    }
}
