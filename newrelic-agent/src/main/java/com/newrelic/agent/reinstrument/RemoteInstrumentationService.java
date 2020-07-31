/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.agent.service.Service;

public interface RemoteInstrumentationService extends Service {

    /**
     * Parses the input XML, creates the new point cuts, and then reinstruments any classes matching the new point cuts.
     *
     * @param xml Xml with new point cuts to be created for future instrumentation.
     * @return Information from the result.
     */
    ReinstrumentResult processXml(String xml);

}
