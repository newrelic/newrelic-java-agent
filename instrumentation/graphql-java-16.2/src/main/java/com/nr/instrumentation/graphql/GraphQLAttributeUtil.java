package com.nr.instrumentation.graphql;

import com.newrelic.agent.bridge.AgentBridge;
import graphql.language.Argument;
import graphql.language.StringValue;

import java.util.List;

public class GraphQLAttributeUtil {
    public static void addAttributeForArgument(List<Argument> arguments){
        for (Argument argument: arguments) {
            AgentBridge.privateApi.addTracerParameter("graphql.field." + argument.getName(), cast(argument));
        }
    }

    private static String cast(Argument argument) {
        //fixme lots of possible types to cast to
        StringValue stringValue = (StringValue) argument.getValue();
        return stringValue.getValue();
    }
}
