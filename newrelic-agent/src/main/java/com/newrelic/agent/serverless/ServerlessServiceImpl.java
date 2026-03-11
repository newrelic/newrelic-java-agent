package com.newrelic.agent.serverless;

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.concurrent.atomic.AtomicReference;

public class ServerlessServiceImpl extends AbstractService implements ServerlessService {
    private final AtomicReference<String> arn = new AtomicReference<>();
    private final AtomicReference<String> functionVersion = new AtomicReference<>();

    public ServerlessServiceImpl() {
        super(ServerlessService.class.getSimpleName());
    }

    @Override
    public void setArn(String arnValue) {
        if (arnValue != null && !arnValue.isEmpty()) {
            arn.set(arnValue);
        }
    }

    @Override
    public void setFunctionVersion(String version) {
        if (version != null && !version.isEmpty()) {
            functionVersion.set(version);
        }
    }

    @Override
    public String getArn() {
        return arn.get();
    }

    @Override
    public String getFunctionVersion() {
        return functionVersion.get();
    }

    @Override
    public boolean isApmLambdaModeEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().isApmLambdaModeEnabled();
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public boolean isEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getServerlessConfig().isEnabled();
    }
}
