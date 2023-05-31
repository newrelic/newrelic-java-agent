/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import com.newrelic.agent.Agent;
import com.newrelic.agent.profile.method.MethodInfo;
import com.newrelic.agent.profile.method.MethodInfoFactory;
import com.newrelic.agent.util.StringMap;

/**
 * This class represents an execution point in the code.
 */
public class ProfiledMethod implements JSONStreamAware {

    private final StackTraceElement stackTraceElement;
    private final int hashCode;
    private MethodInfo info;
    private final IProfile profile;
    private final String methodId;

    /**
     * Parameters are guaranteed to be non-null.
     * 
     * @param stackTraceElement the stack trace element
     */
    private ProfiledMethod(String methodId, IProfile profile, StackTraceElement stackTraceElement) {
        this.profile = profile;
        this.stackTraceElement = stackTraceElement;
        hashCode = stackTraceElement.hashCode();
        this.methodId = methodId;
    }

    /**
     * Create a new instance from a {@link StackTraceElement}
     * @param stackElement the element in a stack trace
     */
    public static ProfiledMethod newProfiledMethod(String methodId, IProfile profile, StackTraceElement stackElement) {
        if (stackElement == null) {
            return null;
        }
        if (stackElement.getClassName() == null) {
            return null;
        }
        if (stackElement.getMethodName() == null) {
            return null;
        }

        return new ProfiledMethod(methodId, profile, stackElement);
    }

    public String getFullMethodName() {
        return getClassName() + ":" + getMethodName();
    }

    public String getMethodName() {
        return stackTraceElement.getMethodName();
    }

    public String getClassName() {
        return stackTraceElement.getClassName();
    }

    public final int getLineNumber() {
        return stackTraceElement.getLineNumber();
    }

    public String getMethodId() {
        return methodId;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProfiledMethod other = (ProfiledMethod) obj;
        return other.stackTraceElement.equals(stackTraceElement);
    }

    @Override
    public String toString() {
        return getFullMethodName() + ":" + getLineNumber();
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONValue.writeJSONString(methodId, out);
    }
    
    public JSONStreamAware getMethodJson() {
        StringMap stringMap = profile.getStringMap();
        final List<Object> data = new ArrayList<>();
        data.add(stringMap.addString(getClassName()));
        data.add(stringMap.addString(getMethodName()));
        data.add(getLineNumber());

        if (info != null) {
            data.add(info.getJsonMethodMaps(stringMap));
        }
        
        return out -> JSONArray.writeJSONString(data, out);
    }
    
    void setMethodInfo(MethodInfo methodInfo) {
        this.info = methodInfo;
    }

    void setMethodDetails(MethodInfoFactory methodInfoFactory) {
        if (null != info) {
            return;
        }
        try {
            info = methodInfoFactory.getMethodInfo(getClassName(), getMethodName(), getLineNumber());
        } catch(Throwable e) {
            Agent.LOG.log(Level.FINER, e, "Error finding MethodInfo for {0}.{1}", getClassName(), getMethodName());
            info = null;
        }
    }

}
