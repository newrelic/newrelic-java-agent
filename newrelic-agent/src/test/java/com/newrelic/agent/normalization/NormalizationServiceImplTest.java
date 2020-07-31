/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.io.IOException;
import java.util.Map;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.IRPMService;
import com.newrelic.weave.utils.Streams;

public class NormalizationServiceImplTest {

    @Before
    public void before() throws Exception {
        AgentHelper.bootstrap(AgentHelper.createAgentConfig(true));
    }

    @Test
    public void test() throws IOException, ParseException {

        String path = '/' + NormalizationServiceImplTest.class.getPackage().getName().replace('.', '/')
                + "/segment_terms.txt";
        String json = new String(Streams.read(NormalizationServiceImplTest.class.getResourceAsStream(path), true));

        Map data = (Map) new JSONParser().parse(json);

        NormalizationServiceImpl service = new NormalizationServiceImpl();

        IRPMService rpmService = Mockito.mock(IRPMService.class);
        AgentConfig config = AgentConfigImpl.createAgentConfig(data);
        service.connected(rpmService, config);
    }
}
