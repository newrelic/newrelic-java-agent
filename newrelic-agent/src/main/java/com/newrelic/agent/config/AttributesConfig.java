/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.List;

public interface AttributesConfig {

    boolean isEnabledRoot();

    boolean isAttsEnabled(AgentConfig config, boolean defaultProp, String... dest);

    List<String> attributesRootInclude();

    List<String> attributesRootExclude();

    /**
     * Whether the old http attributes (httpResponseCode, httpResponseMessage) should be sent.
     * @since 8.8.0
     */
    boolean isLegacyHttpAttr();

    /**
     * Whether the new http attributes (http.statusCode, http.statusText) should be sent.
     * @since 8.8.0
     */
    boolean isStandardHttpAttr();
}
