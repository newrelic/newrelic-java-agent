/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.chat.client;

import com.newrelic.api.agent.LlmTokenCountCallback;

public class TokenCountCallback implements LlmTokenCountCallback {
    @Override
    public int calculateLlmTokenCount(String model, String content) {
        return 13;
    }
}
