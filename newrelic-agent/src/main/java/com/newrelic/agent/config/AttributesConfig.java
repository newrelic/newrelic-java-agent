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

}
