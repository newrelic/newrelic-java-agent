/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import org.objectweb.asm.tree.ClassNode;

import com.newrelic.weave.utils.SynchronizedClassNode;
import com.newrelic.weave.utils.WeaveUtils;

/**
 * Supertype for all error trap handlers.
 * 
 * To create a new error trap you must:
 * 
 * <ol>
 * <li>Think really hard about what you're about to do.</li>
 * <li>Create a class which <b>directly</b> extends this class and implements its own static method with the same
 * signature as {@link ErrorTrapHandler#onWeaverThrow(Throwable t)}</li>
 * <li>Convert the implementing class into a {@link ClassNode} and configure it in {@link WeavePackageConfig}</li>
 * </ol>
 * 
 * The code in onWeaverThrow will be inlined into <b>every</b> target class you're weaving into. This means this code
 * should not do anything stateful or attempt to load classes which will not be visible on all classloaders.
 */
public abstract class ErrorTrapHandler {
    public static final String HANDLER_METHOD_NAME = "onWeaverThrow";
    public static final String HANDLER_METHOD_DESC = "(Ljava/lang/Throwable;)V";

    /**
     * Using this handler will skip error trapping. This is the default handler.
     */
    public static final ClassNode NO_ERROR_TRAP_HANDLER = new SynchronizedClassNode(WeaveUtils.ASM_API_LEVEL) {
    };

    /**
     * This method will be inlined as an exception handler for when weave code throws an exception. This will <b>not</b>
     * handle original code throwing an exception or weave code explicitly calling throw.
     * <p/>
     * 
     * If you do not throw an exception here, the original return value will be returned in the composite code.
     * 
     * @param weaverError the error that was thrown by the weaved code
     * @throws Throwable
     */
    public static void onWeaverThrow(Throwable weaverError) throws Throwable {
    }
}
