/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.Agent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class AttributesNode {

    private static final String END_WILDCARD = "*";

    private final String original;
    private final String name;
    private final boolean hasEndWildcard;
    private final Set<AttributesNode> children;
    private AttributesNode parent;
    private boolean includeDestination;
    private final String destination;
    private final boolean isDefaultRule;

    public AttributesNode(String pOriginal, boolean isIncluded, final String dest, final boolean isDefault) {

        original = pOriginal;
        if (original.endsWith(END_WILDCARD)) {
            name = original.substring(0, original.length() - 1);
            hasEndWildcard = true;
        } else {
            name = pOriginal;
            hasEndWildcard = false;
        }
        includeDestination = isIncluded;
        destination = dest;
        isDefaultRule = isDefault;
        children = new HashSet<>();
        parent = null;
    }

    /**
     * Determines if the input key should be sent to the collector. This can return null.
     * 
     * The method returns a Boolean instead of a boolean because the attributes service has two tries. The first one
     * includes the configuration include and exclude nodes. The second one includes the default excludes. These can not
     * be merged together because the user configuration should always take precedent. The Boolean allows us to
     * determine if there were any matches in the first trie. For example, suppose we want to exclude
     * request.parameters.* be default, but the customer wants to include request.*. In the tree, request.parameters.*
     * would be after request.* meaning it would take precedent, when the request.* should really take precedent.
     * 
     * @param key An attribute key.
     * @return True means the key should be included. False means the key should not be included. Null means the key did
     *         not match any rules in the tree.
     */
    protected Boolean applyRules(String key) {
        Boolean result = null;
        if (matches(key)) {
            logMatch(key);
            result = includeDestination;
            Boolean tmp;
            for (AttributesNode current : children) {
                // should only match one child
                tmp = current.applyRules(key);
                if (tmp != null) {
                    result = tmp;
                    break;
                }
            }
        }
        return result;
    }

    private void logMatch(String key) {
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINEST, "{0}: Attribute key \"{1}\" matched {2} {3} rule \"{4}\"", destination, key,
                    isDefaultRule ? "default" : "config", includeDestination ? "INCLUDE" : "EXCLUDE", original);
        }
    }

    public boolean addNode(AttributesNode rule) {
        if (rule != null) {
            if (isSameString(rule)) {
                mergeIncludeExcludes(rule);
                return true;
            } else if (isInputBefore(rule)) {
                addNodeBeforeMe(rule);
                return true;
            } else if (isInputAfter(rule)) {
                for (AttributesNode current : children) {
                    if (current.addNode(rule)) {
                        return true;
                    }
                }
                // if we get to here then rule has not been added yet
                addNodeToMe(rule);
                return true;
            }
        }
        return false;
    }

    // protected for testing
    protected boolean matches(String key) {
        return (key != null) && (hasEndWildcard ? key.startsWith(name) : name.equals(key));
    }

    protected boolean mightMatch(String key) {
        return (key != null) && (key.startsWith(name) || name.startsWith(key));
    }

    protected boolean isIncludeDestination() {
        return includeDestination;
    }

    private boolean isSameString(AttributesNode rule) {
        return original.equals(rule.original);
    }

    // fo* is before foo and foo*
    private boolean isInputBefore(AttributesNode rule) {
        return rule.hasEndWildcard && name.startsWith(rule.name);
    }

    private boolean isInputAfter(AttributesNode rule) {
        // rule name should be longer than this name
        return hasEndWildcard && rule.name.startsWith(name);
    }

    private void addNodeBeforeMe(AttributesNode rule) {
        AttributesNode rulesParent = this.parent;
        moveChildrenToRuleAsNeeded(parent, rule);
        rulesParent.addNodeToMe(rule);
    }

    private void moveChildrenToRuleAsNeeded(AttributesNode parent, AttributesNode rule) {
        Iterator<AttributesNode> it = parent.children.iterator();
        AttributesNode ar;
        while (it.hasNext()) {
            ar = it.next();
            if (ar.isInputBefore(rule)) {
                // move ar's parent to the rule
                ar.parent = rule;
                // remove the ar from the parent's children list
                it.remove();
                // add to rule's children list
                rule.children.add(ar);
            }
        }
    }

    protected void addNodeToMe(AttributesNode rule) {
        rule.parent = this;
        this.children.add(rule);
    }

    protected boolean mergeIncludeExcludes(AttributesNode rule) {
        // exclude wins over includes
        includeDestination = includeDestination && rule.includeDestination;
        return includeDestination;
    }

    public void printTrie() {
        StringBuilder sb = new StringBuilder("Root: ").append(this.original).append("\n");

        Queue<AttributesNode> q = new LinkedBlockingQueue<>();
        AttributesNode ar = this;
        while (ar != null) {
            sb.append("Parent: ");
            if (ar.parent != null) {
                sb.append(ar.parent.original);
            } else {
                sb.append("null");
            }
            sb.append(" This: ").append(ar.original).append(" Children: ");
            if (children != null) {
                for (AttributesNode c : ar.children) {
                    sb.append(" ").append(c.original);
                    q.add(c);
                }
            }
            ar = q.poll();
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    // this should only be used for testing
    protected AttributesNode getParent() {
        return parent;
    }

    protected Set<AttributesNode> getChildren() {
        return children;
    }

}
