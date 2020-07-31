/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.CommandParserConfig;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.util.JSONException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * The command parser parses commands received from the RPM service before metric harvests.
 */
public class CommandParser extends AbstractService implements HarvestListener, AgentConfigListener {

    public CommandParser() {
        super(CommandParser.class.getSimpleName());
    }

    private final Map<String, Command> commands = new HashMap<>();
    private boolean enabled = true;
    private Set<String> disallowedCommands = new HashSet<>();
    private List<Map<Long, Object>> unsentCommandData = new ArrayList<>();

    /**
     * Adds a command to this parser. Allows services to register their own commands.
     *
     * @param commands
     */
    public void addCommands(Command... commands) {
        for (Command command : commands) {
            this.commands.put(command.getName(), command);
        }
    }

    /**
     * Gets the agent commands from the rpm service, processes them, and returns the command results.
     *
     * @see RPMService#getAgentCommands()
     * @see RPMService#sendCommandResults(Map)
     */
    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        IRPMService rpmService = ServiceFactory.getRPMServiceManager().getOrCreateRPMService(appName);

        for (Iterator<Map<Long, Object>> iterator = unsentCommandData.iterator(); iterator.hasNext(); ) {
            Map<Long, Object> result = iterator.next();
            try {
                rpmService.sendCommandResults(result);
                iterator.remove();
            } catch (HttpError e) {
                if (e.discardHarvestData()) {
                    iterator.remove();
                } else {
                    String msg = MessageFormat.format("Unable to send agent command feedback. Data will be retried " +
                            "on the next harvest. Command results: {0}", result.toString());
                    getLogger().fine(msg);
                }
            } catch (Exception e) {
                iterator.remove();
                String msg = MessageFormat.format("Unable to send agent command feedback. Data will be dropped. " +
                        "Command results: {0}", result.toString());
                getLogger().fine(msg);
            }
        }

        List<List<?>> commands;
        try {
            commands = rpmService.getAgentCommands();
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Unable to get agent commands - {0}", e.toString());
            getLogger().log(Level.FINEST, e, e.toString());
            return;
        }

        Map<Long, Object> commandResults = processCommands(rpmService, commands);
        try {
            rpmService.sendCommandResults(commandResults);
        } catch (HttpError e) {
            if (!e.discardHarvestData()) {
                unsentCommandData.add(commandResults);
                String msg = MessageFormat.format("Unable to send agent command feedback. Data will be retried on the next harvest. Command results: {0}",
                        commandResults.toString());
                getLogger().fine(msg);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to send agent command feedback. Command results: {0}", commandResults.toString());
            getLogger().fine(msg);
        }
    }

    @Override
    public void afterHarvest(String appName) {
    }

    Command getCommand(String name) throws UnknownCommand {
        Agent.LOG.finer(MessageFormat.format("Process command \"{0}\"", name));
        Command c = commands.get(name);
        if (c == null) {
            throw new UnknownCommand("Unknown command " + name);
        }
        return c;
    }

    /**
     * Processes a list of commands and returns a map of command ids to the result of the command.
     *
     * @param commands
     */
    Map<Long, Object> processCommands(IRPMService rpmService, List<List<?>> commands) {
        Map<Long, Object> results = new HashMap<>();
        int count = 0;
        for (List<?> agentCommand : commands) {
            if (agentCommand.size() != 2) {
                // invalid data
                invalidCommand(rpmService, count, "Unable to parse command", agentCommand);
            } else {
                Object id = agentCommand.get(0);
                if (!(id instanceof Number)) {
                    // invalid id
                    invalidCommand(rpmService, count, "Invalid command id " + id, agentCommand);
                } else {
                    try {
                        Map<?, ?> commandMap = (Map<?, ?>) agentCommand.get(1);
                        String name = (String) commandMap.get("name");
                        Map<?, ?> args = (Map<?, ?>) commandMap.get("arguments");
                        if (args == null) {
                            args = Collections.EMPTY_MAP;
                        }
                        if (isCommandAllowed(name)) {
                            Command command = getCommand(name);
                            Object returnValue = command.process(rpmService, args);
                            results.put(((Number) id).longValue(), returnValue);
                            getLogger().finer(MessageFormat.format("Agent command \"{0}\" return value: {1}", name, returnValue));
                        }
                    } catch (Exception e) {
                        getLogger().severe(MessageFormat.format("Unable to parse command : {0}", e.toString()));
                        getLogger().fine(MessageFormat.format("Unable to parse command", e));
                        results.put(((Number) id).longValue(), new JSONException(e));
                    }
                }
            }
            count++;
        }
        return results;
    }

    /**
     * A given command can be selectively disallowed via configuration.
     */
    private boolean isCommandAllowed(String name) {
        if (this.disallowedCommands.contains(name)) {
            getLogger().fine(MessageFormat.format("Agent command \"{0}\" disallowed by configuration", name));
            return false;
        }
        return true;
    }

    /**
     * Handle a command parse error
     *
     * @param index index of the item in the list of commands
     * @param message
     * @param agentCommand
     */
    private void invalidCommand(IRPMService rpmService, int index, String message, List<?> agentCommand) {
        getLogger().severe(MessageFormat.format("Unable to parse command : {0} ({1})", message, agentCommand.toString()));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void doStart() {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        CoreService coreService = ServiceFactory.getCoreService();
        addCommands(new ShutdownCommand(coreService), new RestartCommand());
        updateCommandParserConfig(config);
        if (isEnabled()) {
            ServiceFactory.getHarvestService().addHarvestListener(this);
        } else {
            getLogger().log(Level.CONFIG, "The command parser is disabled");
        }
    }

    private void updateCommandParserConfig(AgentConfig agentConfig) {
        try {
            CommandParserConfig commandParserConfig = agentConfig.getCommandParserConfig();
            if (commandParserConfig != null) {
                if (this.enabled != commandParserConfig.isEnabled()) {
                    this.enabled = commandParserConfig.isEnabled();

                    // If we've gone from disabled to enabled, add this as a harvest listener, otherwise remove it
                    if (this.enabled) {
                        ServiceFactory.getHarvestService().addHarvestListener(this);
                    } else {
                        ServiceFactory.getHarvestService().removeHarvestListener(this);
                    }
                }

                Set<String> disallowedCommands = new HashSet<>();
                for (String command : commandParserConfig.getDisallowedCommands()) {
                    if (!command.equals(ShutdownCommand.COMMAND_NAME) && !command.equals(RestartCommand.COMMAND_NAME)) {
                        disallowedCommands.add(command);
                    }
                }

                this.disallowedCommands = disallowedCommands;
            }
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Unable to parse the command_parser section in newrelic.yml");
        }
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        updateCommandParserConfig(agentConfig);
    }

    @Override
    protected void doStop() {
    }
}
