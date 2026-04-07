/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import java.util.Collection;
import java.util.Collections;

/**
 * <p>A filter that tells if a given key should be included, given include and exclude lists.</p>
 * <p>Items in the list may have wildcards ('*') as the last character.</p>
 * <p>If the include list is empty, then all keys are included.</p>
 */
public class ExcludeIncludeFilterImpl implements ExcludeIncludeFilter {

    private final RootConfigAttributesNode rootNode;

    private final boolean includeByDefault;

    /**
     * @param identifier used in logs so filters can be distinguished
     * @param excludes   the collection of keys to be excluded
     * @param includes   the collection of keys to be included, if empty, all keys are considered included
     */
    public ExcludeIncludeFilterImpl(String identifier, Collection<String> excludes, Collection<String> includes) {
        this(identifier, excludes, includes, includes == null || includes.isEmpty());
    }

    /**
     * @param identifier used in logs so filters can be distinguished
     * @param excludes   the collection of keys to be excluded
     * @param includes   the collection of keys to be included
     * @param includeByDefault if true, keys not in the include list will be included, otherwise they will be excluded
     */
    public ExcludeIncludeFilterImpl(String identifier, Collection<String> excludes, Collection<String> includes, boolean includeByDefault) {
        rootNode = new RootConfigAttributesNode(identifier);

        this.includeByDefault = includeByDefault;

        boolean notDefault = false;

        if (includes != null) {
            for (String key : includes) {
                AttributesNode includeNode = new AttributesNode(key, true, identifier, notDefault);
                rootNode.addNode(includeNode);
            }
        }

        if (excludes != null) {
            for (String key : excludes) {
                AttributesNode excludeNode = new AttributesNode(key, false, identifier, notDefault);
                rootNode.addNode(excludeNode);
            }
        }
    }

    @Override
    public boolean shouldInclude(String key) {
        Boolean apply = rootNode.applyRules(key);
        // apply can be null, which means there was no match for the key. In this case, the default value will be used.
        return apply == null ? includeByDefault : apply;
    }
}
