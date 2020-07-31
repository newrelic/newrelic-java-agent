/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.postgresql.util;

import com.newrelic.api.agent.weaver.Weave;

/**
 * This is here to prevent 9.4-1207 from applying since this only exists in 1208 and above
 */
@Weave
public class ObjectFactory {

}
