/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * This class determines if a stack trace should be counted as runnable for profiling purposes.
 * Instances are thread safe.
 */
public class RunnableThreadRules {
    
    /**
     * All methods for these classes are considered non-runnable.
     */
    private final Set<String> nonRunnableClasses =
            ImmutableSet.of("jrockit.net.SocketNativeIO");
    
    /**
     * These class methods are considered non-runnable.
     */
    private final Map<String, String> classNameToNonRunnableMethodName = 
            ImmutableMap.<String,String>builder().
                put(Object.class.getName(),"wait").
                put("java.lang.UNIXProcess","waitForProcessExit").
                put("sun.misc.Unsafe","park").
                put("org.apache.tomcat.jni.Socket","accept").
                put("org.apache.tomcat.jni.Poll","poll").
                put("weblogic.socket.PosixSocketMuxer","poll").
                put("weblogic.socket.NTSocketMuxer","getIoCompletionResult").
                put("com.caucho.vfs.JniServerSocketImpl","nativeAccept").build();

    public boolean isRunnable(ThreadInfo threadInfo) {
        if (!Thread.State.RUNNABLE.equals(threadInfo.getThreadState())) {
            return false;
        }
        return isRunnable(threadInfo.getStackTrace());
    }

    public boolean isRunnable(StackTraceElement[] elements) {
        if (elements.length == 0) {
            return false;
        }
        return isRunnable(elements[0]);
    }

    public boolean isRunnable(StackTraceElement firstElement) {

        String className = firstElement.getClassName();
        
        if (nonRunnableClasses.contains(className)) {
            return false;
        }
        
        String methodName = firstElement.getMethodName();
        
        String nonRunnableMethodName = classNameToNonRunnableMethodName.get(className);
        if (methodName.equals(nonRunnableMethodName)) {
            return false;
        }
        
        // if you move this line above the non-runnable thread methods check it'll return true for some Object.wait implementations
        if (!firstElement.isNativeMethod()) {
            return true;
        }
        
        if (className.startsWith("java.io.")) {
            return false;
        }
        if (className.startsWith("java.net.")) {
            return false;
        }
        if (className.startsWith("sun.nio.")) {
            return false;
        }

        return true;
    }

}