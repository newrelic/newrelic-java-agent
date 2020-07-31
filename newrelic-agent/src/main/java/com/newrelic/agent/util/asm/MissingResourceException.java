/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.io.IOException;

public class MissingResourceException extends IOException {

    private static final long serialVersionUID = 1177827391206078775L;

    public MissingResourceException(String message) {
        super(message);
    }

}
