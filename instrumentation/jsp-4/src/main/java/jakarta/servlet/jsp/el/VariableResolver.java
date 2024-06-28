/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package jakarta.servlet.jsp.el;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Prevent the jsp-4 module from applying if this interface is present.
 */
@SkipIfPresent
public interface VariableResolver {
}
