package com.nr.ratpack.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.nr.ratpack.instrumentation.handlers.Spectator;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ratpack.server.RatpackServer;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ratpack" })
public class RatpackServerTest {

    @Test
    public void testServerInfo() throws Exception {
        try (EmbeddedApp app = EmbeddedApp.fromHandler(new Spectator())) {
            RatpackServer server = app.getServer();
            server.start();

            server.stop();

            Introspector introspector = InstrumentationTestRunner.getIntrospector();

            assertEquals("Ratpack", introspector.getDispatcher());
            assertEquals("1.6.0", introspector.getDispatcherVersion());

            assertNotNull(introspector.getServerPort());
            assertEquals(server.getBindPort(), introspector.getServerPort().intValue());
        }
    }

}
