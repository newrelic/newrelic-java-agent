/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public class CustomRequestHeaderConfigImpl implements CustomRequestHeaderConfig {

    private final String customRequestHeader;
    private final String requestHeaderAlias;

    public CustomRequestHeaderConfigImpl(String customRequestHeader, String requestHeaderAlias){
        this.customRequestHeader = customRequestHeader;
        this.requestHeaderAlias = requestHeaderAlias;
    }

    @Override
    public String getHeaderName() { return this.customRequestHeader; }

    @Override
    public String getHeaderAlias() { return this.requestHeaderAlias; }
}
