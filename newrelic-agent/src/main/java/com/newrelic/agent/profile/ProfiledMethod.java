/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import com.newrelic.agent.profile.method.MethodInfo;
import com.newrelic.agent.profile.method.MethodInfoUtil;

/**
 * This class represents an execution point in the code.
 */
public class ProfiledMethod implements JSONStreamAware {

    private final StackTraceElement stackTraceElement;
    private final int hashCode;
    private MethodInfo info;

    /**
     * Parameters are guaranteed to be non-null.
     * 
     * @param stackTraceElement the stack trace element
     */
    private ProfiledMethod(StackTraceElement stackTraceElement) {
        this.stackTraceElement = stackTraceElement;
        hashCode = stackTraceElement.hashCode();
    }

    /**
     * Create a new instance from a {@link StackTraceElement}
     * 
     * @param stackElement the element in a stack trace
     */
    public static ProfiledMethod newProfiledMethod(StackTraceElement stackElement) {
        if (stackElement == null) {
            return null;
        }
        if (stackElement.getClassName() == null) {
            return null;
        }
        if (stackElement.getMethodName() == null) {
            return null;
        }

        return new ProfiledMethod(stackElement);
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
        if (info == null) {
            JSONArray.writeJSONString(Arrays.asList(getClassName(), getMethodName(), getLineNumber()), out);
        } else {
            JSONArray.writeJSONString(Arrays.asList(getClassName(), getMethodName(), getLineNumber(),
                    info.getJsonMethodMaps()), out);
        }

    }

    void setMethodDetails(Map<String, Class<?>> classMap) {
        Class<?> declaringClass = classMap.get(getClassName());
        if (declaringClass != null) {
            try {
                info = MethodInfoUtil.createMethodInfo(declaringClass, getMethodName(), getLineNumber());
            } catch(Throwable e) {
                Agent.LOG.log(Level.FINER, e, "Error finding MethodInfo for {0}.{1}", declaringClass.getName(), getMethodName());
                info = null;
            }
        }
    }

}
