/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.helper;

import com.newrelic.api.agent.Token;

/**
 * A simple holder class so we can easily access and modify the token from classes outside of the hystrix package
 */
public class TokenHolder {

    public volatile Token token;

}
