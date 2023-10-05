package com.nr.instrumentation.kafka.config;

import com.newrelic.agent.ThreadService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigEmitterBehaviorTest {


    private static final Duration SHORT_DELAY = Duration.ofSeconds(2);
    private static final Duration LONG_FREQUENCY = Duration.ofDays(8);
    private static final Duration SHORT_DELAY_EMISSION_TIMEOUT = Duration.ofSeconds(5);
    private static final int CLIENT_COUNT_CAP = 20;
    private static final String LOG_MESSAGE_FRAGMENT_CAPPED_CONFIGURATIONS =
            "New clients will not have their configuration state reported";
    private static final String UNIQUE_PRODUCER_CONFIG_PROPERTY = ProducerConfig.LINGER_MS_CONFIG;

    private ConfigEmissionConfiguration config;

    @Mock
    private Agent agent;

    private final TestableConfigEmitter emitter = new TestableConfigEmitter();

    private final List<ConfigTestUtil.SubmittedEvent> submittedEvents = new CopyOnWriteArrayList<>();

    private final List<ConfigTestUtil.LoggedMessage> loggedMessages = new CopyOnWriteArrayList<>();

    private final AtomicInteger nextUniqueNumber = new AtomicInteger();

    @Before
    public void wireAgent() {
        final Logger agentLogger = ConfigTestUtil.mockLogger(loggedMessages);
        when(agent.getLogger()).thenReturn(agentLogger);

        final Insights insights = ConfigTestUtil.customEventsSink(submittedEvents);
        when(agent.getInsights()).thenReturn(insights);
    }

    @After
    public void cleanup() {
        // ordinary execution uses daemon threads, but for testing we'll want to shutdown the executor service
        emitter.shutdown();
    }

    @Test
    public void schedulesEmissions() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = makeDefaultEmitterConfig();
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            assertThat(emitter.scheduledEmissions, is(true));
            awaitImminentEventSubmission(() -> {
                assertEventsContains(submittedEvents, "KafkaProducerConfig", "client-id");
                assertEventsContains(submittedEvents, "KafkaProducerOverriddenDefaultConfig", "client-id");
            });
            assertEquals(2, submittedEvents.size());
        }
    }

    private void assertEventsContains(List<ConfigTestUtil.SubmittedEvent> submittedEvents, String eventType, String clientId) {
        long count = submittedEvents.stream().filter(event -> event.isClientConfig(eventType, clientId)).count();
        assertThat(count, greaterThan(0L));
    }

    @Test
    public void repeatsEmissions() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            final Duration reportingFrequency = Duration.ofSeconds(1);
            config = new ConfigEmissionConfiguration(true, true, false, false, SHORT_DELAY, reportingFrequency, CLIENT_COUNT_CAP);
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            assertTrue(emitter.scheduledEmissions);
            awaitImminentEventSubmission(() -> {
                assertThat(submittedEvents, hasSize(4));
//                assertThat(submittedEvents).haveExactly(2,
//                        new Condition<>(event ->
//                                event.isClientConfig("KafkaProducerConfig", "client-id"),
//                                "2 general events emitted"));
//                assertThat(submittedEvents).haveExactly(2,
//                        new Condition<>(event ->
//                                event.isClientConfig("KafkaProducerOverriddenDefaultConfig", "client-id"),
//                                "2 overridden default events emitted"));
            });
            // make sure it looks a bit like there was some interval between the two pairs of submissions, we're not
            // going to be too picky about the exact timing
//            assertThat(submittedEvents.get(2).submissionTime - submittedEvents.get(0).submissionTime)
//                    .isGreaterThan(1000 * reportingFrequency.getSeconds() / 2);
        }
    }

    @Test
    public void schedulesSslEmissions() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = new ConfigEmissionConfiguration(true, true,true, false, SHORT_DELAY, LONG_FREQUENCY, CLIENT_COUNT_CAP);
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            assertThat(emitter.scheduledEmissions, is(true));
            awaitImminentEventSubmission(() -> {
                assertThat(submittedEvents, hasSize(3));
                assertEventsContains(submittedEvents, "KafkaProducerConfig", "client-id");
                assertEventsContains(submittedEvents, "KafkaProducerOverriddenDefaultConfig", "client-id");
                assertEventsContains(submittedEvents, "KafkaProducerSslConfig", "client-id");
            });
        }
    }

    @Test
    public void schedulesSaslEmissions() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = new ConfigEmissionConfiguration(true, true,false, true, SHORT_DELAY, LONG_FREQUENCY, CLIENT_COUNT_CAP);
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            assertThat(emitter.scheduledEmissions, is(true));
            awaitImminentEventSubmission(() -> {
                assertThat(submittedEvents, hasSize(3));
                assertEventsContains(submittedEvents, "KafkaProducerConfig", "client-id");
                assertEventsContains(submittedEvents, "KafkaProducerOverriddenDefaultConfig", "client-id");
                assertEventsContains(submittedEvents, "KafkaProducerSaslConfig", "client-id");
            });
        }
    }

    @Test
    public void handlesMultipleClients() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = makeDefaultEmitterConfig();
            final String[] clientIds = {"client-id", "other-client-id", "consumer-client-id"};
            for (final String clientId : clientIds) {
                emitter.registerConfiguration(clientId, someProducerConfig(), new ConfigDef());
            }
            assertThat(emitter.scheduledEmissions, is(true));
            awaitImminentEventSubmission(() -> {
                assertThat(submittedEvents, hasSize(clientIds.length * 2));
                for (final String clientId : clientIds) {
                    assertEventsContains(submittedEvents, "KafkaProducerConfig", clientId);
                    assertEventsContains(submittedEvents, "KafkaProducerOverriddenDefaultConfig", clientId);
                }
            });
        }
    }

    @Test
    public void honorsDisabled() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = new ConfigEmissionConfiguration(false, false, false, false, null, null, CLIENT_COUNT_CAP);
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            assertThat(emitter.scheduledEmissions, is(false));
        }
    }

    @Test
    public void capsRegistrations() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = makeDefaultEmitterConfig();
            for (int i = 0; i < CLIENT_COUNT_CAP; ++i) {
                emitter.registerConfiguration("client-id-" + i, someProducerConfig(), new ConfigDef());
            }
            assertThat(emitter.scheduledEmissions, is(true));
            // all of those registrations should have succeeded, so no complaints should have been registered
//            assertThat(loggedMessages).noneMatch(m -> m.pattern.contains(LOG_MESSAGE_FRAGMENT_CAPPED_CONFIGURATIONS));
            // register some new clients -- their registrations should be ignored and a warning logged
            for (int i = CLIENT_COUNT_CAP; i < CLIENT_COUNT_CAP * 2; ++i) {
                emitter.registerConfiguration("client-id-" + i, someProducerConfig(), new ConfigDef());
            }
            // only a single message should have been logged (we don't want to spam logs)
//            assertThat(loggedMessages)
//                    .haveExactly(1, new Condition<>(
//                            m -> m.level == Level.WARNING &&
//                                    m.pattern.contains(LOG_MESSAGE_FRAGMENT_CAPPED_CONFIGURATIONS),
//                            "warning about configuration caps"));
            awaitImminentEventSubmission(
                    () -> assertThat(submittedEvents, hasSize(CLIENT_COUNT_CAP * 2))
            );
        }
    }

    @Test
    public void updatesEmissionsForResubmissionsUnderCapping() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = makeDefaultEmitterConfig();
            for (int i = 0; i < CLIENT_COUNT_CAP; ++i) {
                emitter.registerConfiguration("client-id-" + i, someProducerConfig(), new ConfigDef());
            }
            final ProducerConfig reregisteredConfig0 = someProducerConfig();
            emitter.registerConfiguration("client-id-0", reregisteredConfig0, new ConfigDef());
            assertThat(emitter.scheduledEmissions, is(true));
            // the re-registration of an existing clientId should be respected, even under capping
//            assertThat(loggedMessages).noneMatch(m -> m.pattern.contains(LOG_MESSAGE_FRAGMENT_CAPPED_CONFIGURATIONS));
            // quick attempt to confirm that we are indeed at the cap threshold
            emitter.registerConfiguration("one-more-config", reregisteredConfig0, new ConfigDef());
            // ... so now the message appears
//            assertThat(loggedMessages).anyMatch(m -> m.pattern.contains(LOG_MESSAGE_FRAGMENT_CAPPED_CONFIGURATIONS));

            // the submitted event for client-id-0 should correspond to the config from the second registration.
            // knowing that each producer config will bear a unique value for the unique property, this is testable
            awaitImminentEventSubmission(() -> {
                assertThat(submittedEvents, hasSize(CLIENT_COUNT_CAP * 2));
                final Optional<ConfigTestUtil.SubmittedEvent> reregisteredClientEvent = submittedEvents.stream()
                        .filter(e -> e.eventType.equals("KafkaProducerConfig") &&
                                e.hasAttribute("clientId", "client-id-0"))
                        .findFirst();
                assertThat(reregisteredClientEvent.isPresent(), is(true));
//                assertThat(reregisteredClientEvent
//                        .get().attributes.get(ConfigScope.GENERAL.configEventAttrName(UNIQUE_PRODUCER_CONFIG_PROPERTY)))
//                        .isEqualTo(reregisteredConfig0.values().get(UNIQUE_PRODUCER_CONFIG_PROPERTY));
            });
        }
    }

    @Test
    public void reregistrationsOfSomeConfigKeyIsNoticed() {
        try (final MockedNrStatics ignored = wireStaticMocks()) {
            config = makeDefaultEmitterConfig();
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
            final String warningMessageFragment = "There have been multiple constructions";
//            assertThat(loggedMessages).noneMatch(m -> m.pattern.contains(warningMessageFragment));
            // a third registration is where we expect a warning to be raised
            emitter.registerConfiguration("client-id", someProducerConfig(), new ConfigDef());
//            assertThat(loggedMessages).anyMatch(m -> m.level == Level.WARNING && m.pattern.contains(warningMessageFragment));

            awaitImminentEventSubmission(() -> {
                assertThat(submittedEvents, hasSize(2)); // general + overridden defaults
            });
        }
    }

    @Test
    public void kafkaConfigKeyIsSane() {
        final String clientId = "clientId";
        final ProducerConfig referenceConfig = someProducerConfig();
        final KafkaConfigKey key = new KafkaConfigKey(clientId, referenceConfig, ConfigScope.GENERAL);
        assertThat(key, equalTo(new KafkaConfigKey(clientId, someProducerConfig(), ConfigScope.GENERAL)));
        assertThat(key, equalTo(new KafkaConfigKey(clientId, referenceConfig, ConfigScope.GENERAL)));
        assertThat(key, not(equalTo(new KafkaConfigKey(clientId, referenceConfig, ConfigScope.SSL))));
        assertThat(key, not(equalTo(new KafkaConfigKey(clientId + "no", referenceConfig, ConfigScope.GENERAL))));
        assertThat(key, not(equalTo(new KafkaConfigKey(clientId, someConsumerConfig(), ConfigScope.GENERAL))));
        assertThat(key, equalTo(key));
        assertThat(key, not(equalTo("pickle")));
        assertThat(key, not(equalTo(null)));
    }

    private ConfigEmissionConfiguration makeDefaultEmitterConfig() {
        return new ConfigEmissionConfiguration(true, true, false, false, SHORT_DELAY, LONG_FREQUENCY, CLIENT_COUNT_CAP);
    }

    private ProducerConfig someProducerConfig() {
        final Map<String, Object> props = new HashMap<>();
        props.put("key.serializer", ByteArraySerializer.class.getName());
        props.put("value.serializer", ByteArraySerializer.class.getName());
        // each config will have a different linger.ms, which can be useful for distinguishing between different
        // configs
        props.put(UNIQUE_PRODUCER_CONFIG_PROPERTY, nextUniqueNumber.getAndIncrement());
        return new ProducerConfig(props);
    }

    private ConsumerConfig someConsumerConfig() {
        final Map<String, Object> props = new HashMap<>();
        props.put("key.deserializer", ByteArrayDeserializer.class.getName());
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        return new ConsumerConfig(props);
    }

    private static class MockedNrStatics implements Closeable {
        private final MockedStatic<ServiceFactory> serviceFactory;

        public MockedNrStatics(final MockedStatic<ServiceFactory> serviceFactory) {
            this.serviceFactory = serviceFactory;
        }

        @Override
        public void close() {
            serviceFactory.close();
        }
    }

    private MockedNrStatics wireStaticMocks() {
        // the thread construction technique requires the thread service be available
        final MockedStatic<ServiceFactory> serviceFactoryMock = Mockito.mockStatic(ServiceFactory.class);
        final ThreadService threadService = new ThreadService();
        serviceFactoryMock.when(ServiceFactory::getThreadService).thenReturn(threadService);
        return new MockedNrStatics(serviceFactoryMock);
    }
    
    private void awaitImminentEventSubmission(final ThrowingRunnable assertion) {
        Awaitility.await().pollInterval(Duration.ofMillis(10)).timeout(SHORT_DELAY_EMISSION_TIMEOUT).untilAsserted(assertion);
    }

    private class TestableConfigEmitter extends ConfigEmitter {

        private boolean scheduledEmissions = false;

        @Override
        protected ConfigEmissionConfiguration readConfiguration() {
            return config;
        }

        @Override
        protected void scheduleEmissions() {
            super.scheduleEmissions();
            scheduledEmissions = true;
        }

        @Override
        protected Agent getAgent() {
            return agent;
        }

        void shutdown() {
            if (emissionExecutor != null) {
                emissionExecutor.shutdownNow();
            }
        }
    }
}
