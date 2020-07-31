/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.web.rewrite;

import com.newrelic.api.agent.weaver.Weave;

/**
 * JBoss 7 is built using Tomcat 6 classes.
 * 
 * This weaved class exists solely to prevent jboss-7 loading in Tomcat.
 */
@Weave
public abstract class Resolver {

}
