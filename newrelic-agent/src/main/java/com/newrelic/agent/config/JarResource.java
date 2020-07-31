/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface JarResource extends Closeable {

    InputStream getInputStream(String name) throws IOException;

    long getSize(String name);

}
