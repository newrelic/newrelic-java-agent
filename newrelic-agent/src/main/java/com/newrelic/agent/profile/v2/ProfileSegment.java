/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import org.json.simple.JSONStreamAware;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class tracks thread sampling data for an execution point. It records the number of times an execution point was
 * hit, and the called execution points.
 */
public abstract class ProfileSegment implements JSONStreamAware {
    
    protected final ProfiledMethod method;
    protected final ConcurrentMap<ProfiledMethod, ProfileSegment> children;
    
    public ProfileSegment(ProfiledMethod method) {
        super();
        this.method = method;
        this.children = new ConcurrentHashMap<>();
    }
    
    public final Set<ProfiledMethod> getMethods() {
        Set<ProfiledMethod> methods = new HashSet<>();
        methods.add(getMethod());
        for (ProfileSegment kid : children.values()) {
            methods.addAll(kid.getMethods());
        }
        return methods;
    }
    
    public final Collection<ProfileSegment> getChildren() {
        return children.values();
    }
    
    Map<ProfiledMethod, ProfileSegment> getChildMap() {
        return children;
    }
    
    public final ProfiledMethod getMethod() {
        return method;
    }

    /**
     * Remove a child segment from this segment.
     * 
     * @param method the {@link ProfiledMethod} for the child
     */
    protected final void removeChild(ProfiledMethod method) {
        children.remove(method);
    }
    
    /**
     * Add a child segment to this segment if it doesn't already exist.
     * 
     * @param method the {@link ProfiledMethod} for the child
     * @return the {@link ProfileSegment} for the child
     */
    @SuppressWarnings("unchecked")
    public final <P extends ProfileSegment> P addChild(ProfiledMethod method) {
        P result = (P) children.get(method);
        if (result == null) {
            result = createProfileSegment(method);
            ProfileSegment previous = children.putIfAbsent(method, result);
            if (null != previous) {
                return (P) previous;
            }
        }
        return result;
    }
    
    protected abstract <P extends ProfileSegment> P createProfileSegment(ProfiledMethod method);

    public abstract int getCallCount(ProfiledMethod method);
    public abstract int getCallSiteCount();
    public abstract void incrementCallCount(boolean runnable);
    protected abstract int getRunnableCallCount();
}
