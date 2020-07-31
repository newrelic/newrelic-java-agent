/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.core;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// This class only exists in <= 6.4.0 so we use it to prevent 6.4.0 from applying to 5.1.0
@SkipIfPresent(originalName = "org.apache.solr.core.JarRepository")
public class JarRepository_Instrumentation {
}
