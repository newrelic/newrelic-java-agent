/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Collections;
import java.util.Map;

public interface StringMap {
    StringMap NO_OP_STRING_MAP = new StringMap() {

        @Override
        public Object addString(String string) {
            return string;
        }

        @Override
        public Map<Object, String> getStringMap() {
            return Collections.emptyMap();
        }

    };

    Object addString(String string);

    Map<Object, String> getStringMap();
}