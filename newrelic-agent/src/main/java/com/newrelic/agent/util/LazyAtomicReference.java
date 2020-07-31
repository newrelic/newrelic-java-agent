/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An atomic reference that is lazily initialized by a Callable if an attempt is made to read the value
 * before it has been set. Null values are not allowed, so the initializing Callable must not return null.
 * The implementation guarantees that the initial value will never overwrite a value set through a setter
 * and that the value will be initialized at most once. The implementation cannot guarantee the initializer
 * will only be <em>invoked<em> only once, or that it will be invoked at all; thus the initializer should
 * not have side effects.
 * 
 * @param <T> the type of the referenced object.
 */
public class LazyAtomicReference<T> {
    // Since we do not expose nulls at the interface of this class and do not allow the initializer to
    // return null, we can use null internally to indicate that a call to the initializer is required.
    // Consequence: changing the way we handle nulls in this class would be really messy.
    private final AtomicReference<T> value = new AtomicReference<>();
    private final Callable<T> initializer;
    
    /**
     * Create an atomic that will be lazily initialized if get is invoked before set.
     * 
     * @param initializer the lazy initializer. The initializer is only invoked if a client class attempts
     * to get the value before it has been set. The initializer must not return null and must not invoke
     * methods on the instance it is initializing.
     */
    public LazyAtomicReference(Callable<T> initializer) {
        this.initializer = initializer;
    }
    
    /**
     * Get the value of the atomic. If the value has not been set, the initializer is invoked to provide
     * a default value. The implementation provides certain guarantees described in the class comment.
     * 
     * @return the value of the atomic, never null.
     * @throws NullPointerException - the initializer was invoked but returned null; internal error.
     * @throws RuntimeException - wraps any exception thrown in the initializer; internal error.
     */
    public T get() {
        T result = value.get();
        if (result != null) {
            return result;
        }
        
        initializeAtomically();
        return value.get();
    }
            
    /**
     * Set the value of the atomic.
     * 
     * @param newValue the value. May not be null.
     */
    public void set(T newValue) {
        if (newValue == null) {
            throw new NullPointerException("value may not be null");
        }
        this.value.set(newValue);
    }
        
    // Invoke the initializer. When this method returns normally, the value of this instance will be
    // non-null. But the value might not be the one returned by the initializer, because we may "lose
    // the race" with a concurrent setter. So the caller must always re-get the value of the atomic.
    private void initializeAtomically() {
        T proposedInitialValue;
        try {
            proposedInitialValue = initializer.call();
        } catch (Exception e) {
            // I don't want to pollute the interface of get() with checked exceptions,
            // so I have to fall back on an exception that's not very descriptive. 
            throw new RuntimeException("exception in lazy initializer", e);
        }
        if (proposedInitialValue == null) {
            throw new NullPointerException("lazy initializer returned null");
        }      
        
        value.compareAndSet(null, proposedInitialValue);
    }        
}
