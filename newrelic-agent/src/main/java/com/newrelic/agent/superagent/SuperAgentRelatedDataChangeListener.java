package com.newrelic.agent.superagent;

public interface SuperAgentRelatedDataChangeListener {
    public enum Type {
        InstanceId,
        Error,

    }
}
