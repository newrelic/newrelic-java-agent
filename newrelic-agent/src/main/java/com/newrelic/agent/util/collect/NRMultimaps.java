/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.collect;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.ForwardingSetMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

public class NRMultimaps {

    /**
     * Wraps a SetMultimap so that the call to get() first calls containsKey(), and if the key isn't present an
     * immutable set is returned. The set creation side effect of get() is awful. Thanks Obama!
     * 
     * @param multimap
     */
    public static final <K, V> SetMultimap<K, V> performantSetMultimapFrom(final SetMultimap<K, V> multimap) {
        return new ForwardingSetMultimap<K, V>() {
            @Override
            public Set<V> get(K key) {
                return (delegate().containsKey(key)) ? delegate().get(key) : ImmutableSet.<V> of();
            }

            @Override
            protected SetMultimap<K, V> delegate() {
                return multimap;
            }
        };
    }

    public static final <K, V> Multimap<K, V> performantMultimapFrom(final Multimap<K, V> multimap) {
        return new ForwardingMultimap<K, V>() {
            @Override
            public Collection<V> get(K key) {
                return (delegate().containsKey(key)) ? delegate().get(key) : ImmutableList.<V> of();
            }

            @Override
            protected Multimap<K, V> delegate() {
                return multimap;
            }

        };
    }
}
