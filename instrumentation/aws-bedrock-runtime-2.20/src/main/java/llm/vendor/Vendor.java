/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.vendor;

public class Vendor {
    public static final String VENDOR = "bedrock";
    // Bedrock vendor_version isn't obtainable, so set it to instrumentation version instead
    public static final String VENDOR_VERSION = "2.20";
    public static final String BEDROCK = "Bedrock";
    public static final String INGEST_SOURCE = "Java";
}
