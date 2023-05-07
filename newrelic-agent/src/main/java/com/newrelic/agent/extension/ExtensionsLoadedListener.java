/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.io.File;
import java.util.Set;

public interface ExtensionsLoadedListener {
    void loaded(Set<File> extensions);

    ExtensionsLoadedListener NOOP = extensions -> {
    };
}
