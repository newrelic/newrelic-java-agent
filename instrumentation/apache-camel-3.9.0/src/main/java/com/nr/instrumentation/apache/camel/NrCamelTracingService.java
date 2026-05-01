package com.nr.instrumentation.apache.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.StaticService;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.support.service.ServiceHelper;


public class NrCamelTracingService extends ServiceSupport implements StaticService {

    private final CamelContext camelContext;
    private final NrCamelEventNotifier eventNotifier = new NrCamelEventNotifier();

    public NrCamelTracingService(CamelContext camelContext) {
        ObjectHelper.notNull(camelContext, "CamelContext", this);
        this.camelContext = camelContext;
    }

    @Override
    protected void doStart() throws Exception {
        eventNotifier.setIgnoreExchangeCreatedEvent(false);
        eventNotifier.setIgnoreExchangeCompletedEvent(false);
        camelContext.getManagementStrategy().addEventNotifier(eventNotifier);

        ServiceHelper.startService(eventNotifier);
    }

    @Override
    protected void doStop() throws Exception {
        // stop event notifier
        camelContext.getManagementStrategy().removeEventNotifier(eventNotifier);
        ServiceHelper.stopService(eventNotifier);

        // remove route policy
        camelContext.getRoutePolicyFactories().remove(this);
    }
}
