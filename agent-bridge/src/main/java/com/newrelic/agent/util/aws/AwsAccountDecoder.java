/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.aws;

/**
 * Allows for different implementations of Decoder.
 * Either a real decoder or a noop one.
 */
@FunctionalInterface
interface AwsAccountDecoder {
    Long decodeAccount(String accessKey);
}
