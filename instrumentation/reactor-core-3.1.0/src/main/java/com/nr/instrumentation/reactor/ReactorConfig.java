package com.nr.instrumentation.reactor;

import com.newrelic.api.agent.NewRelic;

public class ReactorConfig {
    public static final boolean errorsEnabledNetty = NewRelic.getAgent().getConfig().getValue("reactor-netty.errors.enabled", false);
    public static final boolean errorsRectorEnabled = NewRelic.getAgent().getConfig().getValue("reactor.errors.enabled", false);
    public static final boolean errorsEnabled = errorsRectorEnabled || errorsEnabledNetty;

    private ReactorConfig() {
    }
}
