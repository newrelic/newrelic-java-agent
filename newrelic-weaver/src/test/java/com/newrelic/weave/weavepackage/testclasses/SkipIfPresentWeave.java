/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.BeforeClass;

import com.newrelic.api.agent.weaver.SkipIfPresent;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.violation.WeaveViolationType;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

/**
 * SkipIfPresentWeave.java
 */
@SkipIfPresent(originalName = "com.newrelic.weave.weavepackage.testclasses.SkipIfPresentOriginal")
public class SkipIfPresentWeave {
}
