package com.nr.ratpack.instrumentation;

import com.newrelic.agent.introspec.ErrorEvent;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ratpack.error.ServerErrorHandler;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ratpack" })
public class RatpackErrorHandlerTest {

    @Test
    public void testErrorHandler() throws Exception {
        try (EmbeddedApp app = EmbeddedApp.of(definition ->
                definition.registryOf(
                        registrySpec -> registrySpec.add(ServerErrorHandler.TYPE, new CustomServerErrorHandler()))
                        .handlers(chain -> chain.all(new ThrowExceptionHandler())))) {

            app.getServer().start();
            ReceivedResponse response = app.getHttpClient().get();
            assertEquals(500, response.getStatusCode());
            app.getServer().stop();

            final Introspector introspector = InstrumentationTestRunner.getIntrospector();
            final String expectedTxnName = "WebTransaction/Ratpack/NettyHandlerAdapter.newRequest";
            final Collection<ErrorEvent> errorEventsForTransaction = introspector.getErrorEventsForTransaction(
                    expectedTxnName);
            assertEquals(1, errorEventsForTransaction.size());
            final ErrorEvent error = errorEventsForTransaction.iterator().next();
            assertEquals("java.lang.RuntimeException", error.getErrorClass());
            assertEquals("style bagel", error.getAttributes().get("portland"));
        }
    }
}
