/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe provider for Lambda-specific metadata.
 *
 * This class stores metadata captured from Lambda Context by instrumentation
 * and makes it available to the DataSenderServerlessImpl for inclusion in
 * serverless payloads via reflection.
 *
 * Note: This class is accessed by DataSenderServerlessImpl using reflection
 * to avoid circular module dependencies.
 */
public class LambdaMetadataProvider {

    private static final AtomicReference<String> arn = new AtomicReference<>();
    private static final AtomicReference<String> functionVersion = new AtomicReference<>();

    public static void setArn(String arnValue) {
        arn.set(arnValue);
    }

    public static String getArn() {
        return arn.get();
    }

    public static void setFunctionVersion(String version) {
        functionVersion.set(version);
    }

    public static String getFunctionVersion() {
        return functionVersion.get();
    }

    /**
     * Clears all stored metadata.
     * Primarily used for testing purposes.
     */
    public static void clear() {
        arn.set(null);
        functionVersion.set(null);
    }
}
