/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.rpm;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.service.Service;

public interface RPMConnectionService extends Service {

    void connect(IRPMService rpmService);

    void connectImmediate(IRPMService rpmService);

}
