/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import java.net.URI;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.GenericParameters} instead.
 *
 * Parameters required to report a basic external call using {@link TracedMethod}'s reportAsExternal.
 */
@Deprecated
public class GenericParameters extends com.newrelic.api.agent.GenericParameters implements ExternalParameters {

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.GenericParameters#GenericParameters} instead.
     *
     * @param library
     * @param uri
     * @param procedure
     */
    @Deprecated
    protected GenericParameters(String library, URI uri, String procedure) {
        super(library, uri, procedure);
    }

}
