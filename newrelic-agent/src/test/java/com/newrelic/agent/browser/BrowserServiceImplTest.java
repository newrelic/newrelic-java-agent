package com.newrelic.agent.browser;

import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Test;


import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BrowserServiceImplTest {
    private static final String[] BROWSER_CONFIG_PROPS = {
            AgentConfigImpl.APP_NAME, BrowserConfig.BROWSER_KEY, BrowserConfig.JS_AGENT_LOADER,
            BrowserConfig.JS_AGENT_FILE, BrowserConfig.BEACON, BrowserConfig.ERROR_BEACON, BrowserConfig.APPLICATION_ID
    };

    @Test
    public void isEnabled_returnsTrue() {
        setupConfig("123");
        BrowserServiceImpl bsi = new BrowserServiceImpl();
        assertTrue(bsi.isEnabled());
    }

    @Test
    public void connected_withNullAppName_assignsDefaultBrowserConfig() {
        setupConfig("123");
        MockRPMService mockRPMService = new MockRPMService();
        BrowserServiceImpl bsi = new BrowserServiceImpl();

        // Because the app name will be null from the mockRPMService, the default browser config will be assigned
        // to the generated browser config object with the settings in the setup method
        bsi.connected(mockRPMService, ServiceFactory.getConfigService().getAgentConfig("123"));

        BrowserConfig config = bsi.getBrowserConfig(null);
        Map<String, Object> props = config.getProperties();
        for (String key :props.keySet()) {
            assertEquals("123", props.get(key));
        }
    }

    @Test
    public void connected_withDefaultAppName_assignsDefaultBrowserConfig() {
        setupConfig("123");
        RPMService mockRPMService = mock(RPMService.class);
        when(mockRPMService.getApplicationName()).thenReturn("123");
        BrowserServiceImpl bsi = new BrowserServiceImpl();

        // Because the app name will be "123" from the mockRPMService, the default browser config will be assigned
        // to the generated browser config object with the settings in the setup method
        bsi.connected(mockRPMService, ServiceFactory.getConfigService().getAgentConfig("123"));

        BrowserConfig config = bsi.getBrowserConfig(null);
        Map<String, Object> props = config.getProperties();
        for (String key :props.keySet()) {
            assertEquals("123", props.get(key));
        }
    }

    @Test
    public void connected_withNonDefaultAppName_assignsConfigToBrowserConfigsMap() {
        setupConfig("123");
        RPMService mockRPMService = mock(RPMService.class);
        when(mockRPMService.getApplicationName()).thenReturn("987");

        //This will create the "default" browser config
        BrowserServiceImpl bsi = new BrowserServiceImpl();

        //Now register a config for a different app config
        setupConfig("987");
        bsi.connected(mockRPMService, ServiceFactory.getConfigService().getAgentConfig("987"));

        BrowserConfig config = bsi.getBrowserConfig("987");
        Map<String, Object> props = config.getProperties();
        for (String key :props.keySet()) {
            assertEquals("987", props.get(key));
        }
    }

    private void setupConfig(String val) {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> configMap = new HashMap<>();
        for (String key : BROWSER_CONFIG_PROPS) {
            configMap.put(key, val);
        }

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(configMap), configMap);
        serviceManager.setConfigService(configService);
    }
}
