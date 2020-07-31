/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.language;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.newrelic.weave.weavepackage.language.scala.ScalaAdapter;

/**
 * A place to hold the {@link LanguageAdapter}s.
 */
public class RegisteredLanguageAdapters {
    private static final Set<LanguageAdapter> ADAPTERS;
    static {
        ADAPTERS = Sets.newConcurrentHashSet();
        try {
            RegisteredLanguageAdapters.registerLanguageAdapter(new ScalaAdapter());
        } catch(Throwable t) {
        }
    }

    public static boolean registerLanguageAdapter(LanguageAdapter adapter) {
        return ADAPTERS.add(adapter);
    }

    public static Set<LanguageAdapter> getLanguageAdapters() {
        Set<LanguageAdapter> adapts = ImmutableSet.<LanguageAdapter> builder().addAll(ADAPTERS).build();
        return adapts;
    }
}
