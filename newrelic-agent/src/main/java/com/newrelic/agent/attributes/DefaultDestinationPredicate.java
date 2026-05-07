/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * There should be one of these for each destination. It determines which attributes are allowed for the specific
 * destination.
 *
 * 1. First apply the mandatory rules. These are rules that must be enforced for high security. <br/>
 * 2. If no mandatory match, apply the configuration rules. <br/>
 * 3. If no config matcher, apply the defaults.
 */
public class DefaultDestinationPredicate implements DestinationPredicate {

    private static final int MAX_CACHE_SIZE_BUFFER = 200;
    /**
     * This is always run first. If we match any, then we are done. High security properties go in here
     */
    private final RootConfigAttributesNode mandatoryExcludeTrie;
    /**
     * Contains the properties from the configuration file.
     */
    private final RootConfigAttributesNode configTrie;
    /**
     * Contains the default properties. These are all exclude nodes. These are only run if the mandatory and config are
     * not matched.
     */
    private final AttributesNode defaultExcludeTrie;
    /**
     * Holds recent keys and result values.
     */
    private final Function<String, Boolean> cache;
    /**
     * The destination is mainly used for logging.
     */
    private final String destination;

    DefaultDestinationPredicate(final String dest, final Set<String> exclude, final Set<String> include,
            Set<String> defaultExcludes, Set<String> mandatoryExclude) {

        mandatoryExcludeTrie = generateExcludeConfigTrie(dest, mandatoryExclude);
        configTrie = generateConfigTrie(dest, exclude, include);
        defaultExcludeTrie = generateDefaultTrie(dest, defaultExcludes);
        destination = dest;
        cache = AgentBridge.collectionFactory.memorize(this::isIncluded, MAX_CACHE_SIZE_BUFFER);
    }

    private Boolean isIncluded(String key) {
        // high security rules first
        Boolean output = mandatoryExcludeTrie.applyRules(key);
        if (output == null) {
            // if no match then configuration rules
            output = configTrie.applyRules(key);
        }
        // configuration rules override default rules
        if (output == null) {
            // if no match for a configuration rule, then examine default rules
            output = defaultExcludeTrie.applyRules(key);
        }
        return output;
    }

    /**
     * Returns the result of applying this predicate to {@code key}. This method is <i>generally expected</i>, but not
     * absolutely required, to have the following properties:
     *
     * <ul>
     * <li>Its execution does not cause any observable side effects.
     * <li>The computation is <i>consistent with equals</i>; that is, {@link Object#equals Object.equals}{@code (a, b)}
     * implies that {@code predicate.apply(a) == predicate.apply(b))}.
     * </ul>
     */
    @Override
    public boolean apply(String key) {
        return changeToPrimitiveAndLog(key, cache.apply(key));
    }

    private void logOutput(String key, boolean value) {
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.log(Level.FINER, "{0}: Attribute {1} is {2}", destination, key, value ? "enabled" : "disabled");
        }
    }

    private boolean changeToPrimitiveAndLog(String key, Boolean value) {
        // if no rules were matched then return true - should not be in here if the service is disabled
        boolean out = (value == null ? true : value);
        logOutput(key, out);
        return out;
    }

    public boolean isPotentialConfigMatch(String key) {
        List<AttributesNode> queue = new LinkedList<>(configTrie.getChildren());
        AttributesNode node;
        while (!queue.isEmpty()) {
            node = queue.remove(0);
            queue.addAll(node.getChildren());
            if (node.isIncludeDestination() && node.mightMatch(key)) {
                return true;
            }
        }
        return false;
    }

    static AttributesNode generateDefaultTrie(final String dest, Set<String> defaultExcludes) {
        AttributesNode root = new AttributesNode("*", true, dest, true);
        for (String current : defaultExcludes) {
            root.addNode(new AttributesNode(current, false, dest, true));
        }
        return root;
    }

    private static RootConfigAttributesNode generateExcludeConfigTrie(final String dest, final Set<String> exclude) {
        RootConfigAttributesNode root = new RootConfigAttributesNode(dest);
        addSpecifcInOrEx(root, false, exclude, dest, true);
        return root;
    }

    static RootConfigAttributesNode generateConfigTrie(final String dest, final Set<String> exclude, final Set<String> include) {
        RootConfigAttributesNode root = new RootConfigAttributesNode(dest);
        addSpecifcInOrEx(root, false, exclude, dest, false);
        addSpecifcInOrEx(root, true, include, dest, false);
        return root;
    }

    private static void addSpecifcInOrEx(AttributesNode root, boolean isInclude, Set<String> inOrEx, final String dest, boolean isDefault) {
        for (String current : inOrEx) {
            root.addNode(new AttributesNode(current, isInclude, dest, isDefault));
        }
    }

}
