package com.nr.instrumentation;

import com.newrelic.api.agent.NewRelic;

public class NettyReactorConfig {
    public static final boolean errorsEnabled = NewRelic.getAgent().getConfig().getValue("reactor-netty.errors.enabled", true);

    private NettyReactorConfig() {
    }
}
