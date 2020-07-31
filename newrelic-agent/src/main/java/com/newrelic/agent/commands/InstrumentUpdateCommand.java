/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.reinstrument.ReinstrumentResult;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class InstrumentUpdateCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "instrumentation_update";
    protected static final String ARG_NAME = "instrumentation";
    protected static final String ARG_VALUE_MAP_NAME = "config";

    private final RemoteInstrumentationService service;

    public InstrumentUpdateCommand(RemoteInstrumentationService pService) {
        super(COMMAND_NAME);
        service = pService;
    }

    @Override
    public Map process(IRPMService pRpmService, Map pArguments) throws CommandException {
        Agent.LOG.log(Level.FINE, "Processing an instrumentation update command.");
        if (pArguments == null || pArguments.size() == 0) {
            Agent.LOG.warning(MessageFormat.format("The instrumentation_update command must have at least one argument called {0}.", ARG_NAME));
            throw new CommandException("The instrumentation_update command expected 1 argument.");
        } else {
            String xml = getXmlFromMaps(pArguments);
            if (xml != null) {
                ReinstrumentResult result = service.processXml(xml);
                if (result != null) {
                    return result.getStatusMap();
                }
            }
        }
        return Collections.EMPTY_MAP;
    }

    private static void warnIfArgsLeft(Map pArguments) {
        if (pArguments.size() > 0) {
            Agent.LOG.warning(MessageFormat.format("The instrumentation_update command did not recognize the following arguments: {0}.",
                    pArguments.keySet().toString()));
        }
    }

    protected static String getXmlFromMaps(Map pArguments) {
        Object instrumentationWorkObject = pArguments.remove(ARG_NAME);
        warnIfArgsLeft(pArguments);
        if (instrumentationWorkObject != null) {
            if (instrumentationWorkObject instanceof Map) {
                Map instrumentWorkMap = (Map) instrumentationWorkObject;
                if (instrumentWorkMap != null) {
                    return getXml(instrumentWorkMap);
                }
            } else {
                Agent.LOG.log(Level.INFO, "The agent instrumentation object is not a Map. The XML will not be processed.");
            }
        } else {
            Agent.LOG.log(Level.INFO, "The agent instrumentation object is null. The instrumentation XML will not be processed.");
        }
        return null;
    }

    /**
     * The response should be a map with a key (string) pointing to the XML instrumentation (string).
     */
    private static String getXml(Map instrumentWorkMap) {
        Object xml = instrumentWorkMap.get(ARG_VALUE_MAP_NAME);
        if (xml != null) {
            if (xml instanceof String) {
                return ((String) xml);
            } else {
                Agent.LOG.info(MessageFormat.format("The property {0} was empty meaning no instrumentation update will occur.", ARG_VALUE_MAP_NAME));
            }
        } else {
            Agent.LOG.log(Level.INFO, "The agent instrumentation XML is null. The instrumentation XML will not be processed.");
        }
        return null;
    }

}
