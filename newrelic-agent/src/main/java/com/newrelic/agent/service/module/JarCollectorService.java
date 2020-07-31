/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.service.Service;

/**
 * Interface for collecting and sending jars to RPM.
 * 
 * @since Nov 9, 2012
 */
public interface JarCollectorService extends Service {

    ClassMatchVisitorFactory getSourceVisitor();

}
