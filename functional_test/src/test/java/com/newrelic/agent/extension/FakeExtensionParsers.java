/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.util.List;

public class FakeExtensionParsers extends ExtensionParsers {
    public FakeExtensionParsers(List<ConfigurationConstruct> constructs) {
        super(constructs);
    }
}
