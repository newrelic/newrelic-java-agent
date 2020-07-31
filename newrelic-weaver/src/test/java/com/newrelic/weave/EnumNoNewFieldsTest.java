/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EnumNoNewFieldsTest {

    @Test
    public void testEnumInvalidNewFields() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.EnumNoNewFieldsTest$Day_Weave"));
        WeavePackage weavePackage = new WeavePackage(config, weaveBytes);

        ClassCache cache = new ClassCache(new ClassLoaderFinder(EnumNoNewFieldsTest.class.getClassLoader()));
        PackageValidationResult result = weavePackage.validate(cache);
        WeaveTestUtils.expectViolations(result, new WeaveViolation(WeaveViolationType.ENUM_NEW_FIELD,
                "com/newrelic/weave/EnumNoNewFieldsTest$Day"));
    }

    enum Day {
        M
    }

    @Weave(originalName = "com.newrelic.weave.EnumNoNewFieldsTest$Day")
    enum Day_Weave {
        M;

        @NewField
        int bar;
    }
}
