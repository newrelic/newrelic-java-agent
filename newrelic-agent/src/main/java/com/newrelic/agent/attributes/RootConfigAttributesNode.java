/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

/**
 * All include and excludes from the configuration file should be in this trie. The root node does not check its value.
 * It immediately delegates to its children.
 */
public class RootConfigAttributesNode extends AttributesNode {

    public RootConfigAttributesNode(String dest) {
        super("", true, dest, true);
    }

    @Override
    public Boolean applyRules(String key) {
        // do not look at this node
        Boolean result = null;
        for (AttributesNode current : getChildren()) {
            // should match at most one child
            result = current.applyRules(key);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    @Override
    public boolean addNode(AttributesNode rule) {
        // do not check me - just look at children
        if (rule != null) {
            for (AttributesNode current : getChildren()) {
                if (current.addNode(rule)) {
                    return true;
                }
            }
            // if we get to here then rule has not been added yet
            addNodeToMe(rule);
            return true;
        }
        return false;
    }

}
