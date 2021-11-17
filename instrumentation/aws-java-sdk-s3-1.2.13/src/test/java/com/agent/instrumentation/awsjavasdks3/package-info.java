/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

/**
 * About testing this aws-java-sdk-s3-1.2.13:
 * <ul>
 *     <li>It is not possible to use an embedded s3mock because it brings conflicting AWS libraries.</li>
 *     <li>Even if an external s3mock is used, it returns human readable (indented) XML and this version of the S3 sdk only understands single line XML.</li>
 * </ul>
 * So to run this test you must use a real S3 bucket.
 */
package com.agent.instrumentation.awsjavasdks3;