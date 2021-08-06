package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import graphql.language.Argument;

import java.util.List;

public class GraphQLAttributeUtil {
    public static void addAttributeForArgument(List<Argument> arguments){
        for (Argument argument: arguments) {
            //todo to fully  implement, these attributes have to be excluded by default.
            // This will require changes to agent Destination and AttributeFilter classes
            AgentBridge.privateApi.addTracerParameter("graphql.field.arg." + argument.getName(), argument.getValue().toString());
        }
    }
}
