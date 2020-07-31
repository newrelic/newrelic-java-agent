/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.language;

import java.util.List;

/**
 * A language adapter has a chance to process the input bytes before they are handed to the WeavePackage.<br/>
 * This allows other JVM languages to define their own weaver api by doing the following:
 * <ul>
 * <li>Extending the weaver api to suite the language of choice</li>
 * <li>Using {@link LanguageAdapter#adapt(List)}, translate the input bytes into bytes the Java api can process. This
 * transformation can be defined by the extended api defined in the previous step.</li>
 * <li>Raise any desired custom violations in the returned {@link LanguageAdapterResult}</li>
 * </ul>
 */
public interface LanguageAdapter {
    /**
     * The adapt method will be called before the weave package processes the instrumentation bytes.
     *
     * @param input bytes read from disk
     * @return A {@link LanguageAdapterResult} containing the output bytes and any violations found by the adapter.
     */
    public LanguageAdapterResult adapt(List<byte[]> input);

}