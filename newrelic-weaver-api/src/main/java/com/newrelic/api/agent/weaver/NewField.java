/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is applied to member variables of weaved classes (classes marked with {@link Weave}) so that they are
 * treated as new fields. If this annotation is not present, the agent will expect the referenced field to be present on
 * the original class and will not load the weaved instrumentation if it is not.
 * 
 * New Relic stores these values in a map keyed by the object having the new field. The map uses weak reference
 * semantics. Under normal circumstances, the entry holding the key and value will be made available for garbage
 * collection after all other references to the key, both inside and outside of the map, have been cleared by the
 * garbage collector.
 * 
 * As with all weakly-keyed maps, this automatic cleanup of unused entries cannot occur if a value holds an ordinary
 * object reference, either direct or indirect, to its own key. Therefore, if the value of a new field holds a reference
 * to the key, the programmer using this API is required to assign null to the value at some appropriate life cycle
 * point in order to allow eventual removal of the map entry. If this is not done, the entry will never be removed and
 * the map may grow without bound.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NewField {

}
