/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Externalizable {
    void write(DataOutputStream dataOutput) throws IOException;
}
