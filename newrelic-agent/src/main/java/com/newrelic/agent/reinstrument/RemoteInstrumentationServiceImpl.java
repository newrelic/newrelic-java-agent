/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.InstrumentUpdateCommand;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ReinstrumentConfig;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassesMatcher;
import com.newrelic.agent.instrumentation.context.InstrumentationContextClassMatcherHelper;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

public class RemoteInstrumentationServiceImpl extends AbstractService implements RemoteInstrumentationService, ConnectionListener, AgentConfigListener {

    public static final String INSTRUMENTATION_CONFIG = "instrumentation";
    private static final String CONFIG_KEY = "config";

    private final ReinstrumentConfig reinstrumentConfig;
    private final boolean isEnabled;

    private volatile boolean isLiveAttributesEnabled;
    private volatile String mostRecentXml = null;

    public RemoteInstrumentationServiceImpl() {
        super(RemoteInstrumentationService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        reinstrumentConfig = config.getReinstrumentConfig();
        isEnabled = reinstrumentConfig.isEnabled();
        isLiveAttributesEnabled = reinstrumentConfig.isAttributesEnabled();
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    protected void doStart() throws Exception {
        if (isEnabled) {
            ServiceFactory.getCommandParser().addCommands(new InstrumentUpdateCommand(this));
            ServiceFactory.getRPMServiceManager().addConnectionListener(this);
            ServiceFactory.getConfigService().addIAgentConfigListener(this);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (isEnabled) {
            ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
            ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void connected(IRPMService pRpmService, AgentConfig pConnectionInfo) {
        if (pConnectionInfo != null) {
            Object value = pConnectionInfo.getProperty(INSTRUMENTATION_CONFIG);
            if (value != null && (value instanceof List)) {
                @SuppressWarnings("unchecked")
                List<Map> daMaps = (List<Map>) value;
                for (Map current : daMaps) {
                    Object config = current.get(CONFIG_KEY);
                    if (config != null && config instanceof String) {
                        processXml((String) config);
                    } else {
                        Agent.LOG.info("The instrumentation configuration passed down does not contain a config key.");
                    }
                }
            }
        }
    }

    @Override
    public void disconnected(IRPMService pRpmService) {
    }

    @Override
    public ReinstrumentResult processXml(String pXml) {
        ReinstrumentResult result = new ReinstrumentResult();
        try {
            if (isEnabled) {
                if (ServiceFactory.getCoreService().getInstrumentation().isRetransformClassesSupported()) {
                    AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
                    if (agentConfig.isCustomInstrumentationEditorAllowed()) {
                        mostRecentXml = pXml;
                        if (isAllXmlRemoved(pXml)) {
                            Agent.LOG.info("The XML file is empty. All custom instrumentation will be removed.");
                            updateJvmWithExtension(null, result);
                        } else {
                            Agent.LOG.log(Level.FINE, "Instrumentation modifications received from the server with attributes {0}.",
                                    (isLiveAttributesEnabled ? "enabled" : "disabled"));
                            Extension currentExt = getExtensionAndAddErrors(result, pXml);
                            if (currentExt != null) {
                                updateJvmWithExtension(currentExt, result);
                            }
                        }
                    } else {
                        handleErrorNoInstrumentation(result, "Remote instrumentation is not supported in high security mode.", pXml);
                    }
                } else {
                    handleErrorNoInstrumentation(result, "Retransform classes is not supported on the current instrumentation.", pXml);
                }
            } else {
                handleErrorNoInstrumentation(result, "The Reinstrument Service is currently disabled.", pXml);
            }
        } catch (Exception e) {
            handleErrorPartialInstrumentation(result, "An unexpected exception occurred: " + e.getMessage(), pXml);
        }
        return result;
    }

    private boolean isAllXmlRemoved(String pXml) {
        return ((pXml == null) || (pXml.trim().length() == 0));
    }

    private Extension getExtensionAndAddErrors(ReinstrumentResult result, String pXml) {
        List<Exception> exceptions = new ArrayList<>();
        Extension currentExt = ExtensionDomParser.readStringGatherExceptions(pXml, exceptions);
        ReinstrumentUtils.handleErrorPartialInstrumentation(result, exceptions, pXml);
        return currentExt;
    }

    private void updateJvmWithExtension(Extension ext, ReinstrumentResult result) {
        List<ExtensionClassAndMethodMatcher> pointCuts = null;
        if (ext == null || !ext.isEnabled()) {
            // remove all if the extension is empty or disabled
            pointCuts = Collections.emptyList();
        } else {
            pointCuts = ExtensionConversionUtility.convertToEnabledPointCuts(Arrays.asList(ext), true,
                    InstrumentationType.RemoteCustomXml, isLiveAttributesEnabled);
        }

        result.setPointCutsSpecified(pointCuts.size());
        // this set of classes to retransform will include any classes that matched the last set of matchers AND
        // classes matched with the new matchers
        ClassRetransformer remoteRetransformer = ServiceFactory.getClassTransformerService().getRemoteRetransformer();
        remoteRetransformer.setClassMethodMatchers(pointCuts);

        Class<?>[] allLoadedClasses = ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses();
        InstrumentationContextClassMatcherHelper matcherHelper = new InstrumentationContextClassMatcherHelper();
        Set<Class<?>> classesToRetransform = ClassesMatcher.getMatchingClasses(
                remoteRetransformer.getMatchers(), matcherHelper, allLoadedClasses);
        ReinstrumentUtils.checkClassExistsAndRetransformClasses(result, pointCuts, ext, classesToRetransform);
    }

    private void handleErrorPartialInstrumentation(ReinstrumentResult result, String msg, String pXml) {
        result.addErrorMessage(msg);
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.fine(MessageFormat.format(msg + " This xml being processed was: {0}", pXml));
        }
    }

    private void handleErrorNoInstrumentation(ReinstrumentResult result, String msg, String pXml) {
        result.addErrorMessage(msg);
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.fine(MessageFormat.format(msg + " This xml will not be instrumented: {0}", pXml));
        }
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        boolean attsEnabled = agentConfig.getReinstrumentConfig().isAttributesEnabled();
        if (isLiveAttributesEnabled != attsEnabled) {
            isLiveAttributesEnabled = attsEnabled;
            Agent.LOG.log(Level.FINE, "RemoteInstrumentationService: Remote attributes are {0}",
                    (isLiveAttributesEnabled ? "enabled" : "disabled"));
            if (mostRecentXml != null) {
                processXml(mostRecentXml);
            }
        }
    }
}
