package com.newrelic.agent.ktor;

import com.newrelic.agent.kotlincoroutines.KotlinCoroutinesService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

public class KtorService extends AbstractService {

    public KtorService() {
        super("KtorService");
    }

    @Override
    protected void doStart() throws Exception {
        KotlinCoroutinesService kotlinCoroutinesService = ServiceFactory.getKotlinCoroutinesService();
        kotlinCoroutinesService.addIgnoredFramework("io.ktor");
    }

    @Override
    protected void doStop() throws Exception {
        KotlinCoroutinesService kotlinCoroutinesService = ServiceFactory.getKotlinCoroutinesService();

    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
