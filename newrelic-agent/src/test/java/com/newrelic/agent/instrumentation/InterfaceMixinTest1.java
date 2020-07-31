/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = "com.newrelic.agent.instrumentation.Test1")
public interface InterfaceMixinTest1 {

    @FieldAccessor(fieldName = "field1", existingField = true)
    boolean _nr_getField1();

    @FieldAccessor(fieldName = "field2", fieldDesc = "Ljava/lang/String;", existingField = true)
    Object _nr_getField2();

    @FieldAccessor(fieldName = "field3", existingField = false)
    Object _nr_getField3();

    public String test1();
}
