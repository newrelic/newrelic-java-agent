/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.language;

import com.newrelic.weave.violation.WeaveViolation;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;

/**
 * Holds the adapted bytes and any violations raised during the language adapting.
 */
public interface LanguageAdapterResult {
    /**
     * The adapted bytes which will be given to the WeavePackage.
     */
    List<byte[]> getAdaptedBytes();
    /**
     * Any violations found during adapting.
     */
    List<WeaveViolation> getViolations();
}
