 package com.nr.instrumentation.kafka.config;

 import com.newrelic.api.agent.Agent;
 import com.newrelic.api.agent.Insights;
 import com.newrelic.api.agent.Logger;
 import com.newrelic.api.agent.NewRelic;
 import org.apache.kafka.clients.CommonClientConfigs;
 import org.apache.kafka.clients.admin.AdminClientConfig;
 import org.apache.kafka.clients.consumer.ConsumerConfig;
 import org.apache.kafka.clients.producer.Partitioner;
 import org.apache.kafka.clients.producer.ProducerConfig;
 import org.apache.kafka.common.Cluster;
 import org.apache.kafka.common.config.AbstractConfig;
 import org.apache.kafka.common.config.ConfigDef;
 import org.apache.kafka.common.serialization.ByteArrayDeserializer;
 import org.apache.kafka.common.serialization.ByteArraySerializer;
 import org.apache.kafka.common.utils.AppInfoParser;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.mockito.Answers;
 import org.mockito.ArgumentMatchers;
 import org.mockito.Mock;
 import org.mockito.MockedStatic;
 import org.mockito.Mockito;
 import org.mockito.junit.MockitoJUnitRunner;

 import java.time.Duration;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.function.Supplier;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;

 import static org.hamcrest.Matchers.containsInAnyOrder;
 import static org.hamcrest.Matchers.equalTo;
 import static org.hamcrest.Matchers.hasSize;
 import static org.hamcrest.Matchers.is;
 import static org.hamcrest.Matchers.notNullValue;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertThat;
 import static org.mockito.ArgumentMatchers.any;
 import static org.mockito.ArgumentMatchers.anyString;
 import static org.mockito.ArgumentMatchers.eq;
 import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigEmitterEventContentTest {

    // whatever values are fine here since we explicitly provoke reporting here rather than relying
    // on the scheduled thread
    private static final Duration DELAY = Duration.ofDays(7);
    private static final Duration FREQUENCY = Duration.ofDays(8);

    private static final int CLIENT_COUNT_CAP = 20;

    // plant a default value for this config that is common across all client types
    private static final String SOME_REAL_SHARED_CONFIG_WITH_DEFAULT_VALUE_KEY = CommonClientConfigs.METADATA_MAX_AGE_CONFIG;
    private static final long SOME_REAL_SHARED_CONFIG_DEFAULT_VALUE = 3927L;

    private ConfigEmissionConfiguration config;

    @Mock
    private Agent agent;

    final TestableConfigEmitter emitter = new TestableConfigEmitter();

    @Mock
    private Logger agentLogger;

    private final List<ConfigTestUtil.SubmittedEvent> submittedEvents = new ArrayList<>();

    private final Insights insights = ConfigTestUtil.customEventsSink(submittedEvents);

    private final Map<String,String> envVars = new HashMap<>();

    private static MockedStatic<NewRelic> newRelicMockedStatic;

    @BeforeClass
    public static void beforeClass() {
        List<String> environmentAttrs = Arrays.asList("DEPLOYED_VERSION", "COMMIT_REF");

        newRelicMockedStatic = Mockito.mockStatic(NewRelic.class, Answers.RETURNS_DEEP_STUBS);
        newRelicMockedStatic.when(() -> NewRelic.getAgent().getConfig().getValue(anyString(), any()))
                .thenReturn(environmentAttrs);
    }

    @AfterClass
    public static void afterClass() {
        newRelicMockedStatic.close();
    }

    @Before
    public void wireAgent() {
        when(agent.getLogger()).thenReturn(agentLogger);
        when(agent.getInsights()).thenReturn(insights);
    }

    private static ConfigDef simpleConfigDef() {
        // All the client configs will have picked up the default value for this attribute
        // from their *real* ConfigDefs, but this will be a different default from that established
        // in *this* ConfigDef which is what we're passing in at registration time. This should mean
        // that this configuration value will normally look like it's overriding a default.
        return new ConfigDef().define(
                SOME_REAL_SHARED_CONFIG_WITH_DEFAULT_VALUE_KEY,
                ConfigDef.Type.LONG,
                SOME_REAL_SHARED_CONFIG_DEFAULT_VALUE,
                ConfigDef.Importance.HIGH,
                "some doc");
    }

    @Test
    public void producerEventsHaveExpectedShape() {
        config = makeConfigWithSslAndSasl();
        final String clientId = "producer";
        final ProducerConfig clientConfig = producerConfig();
        emitter.registerConfiguration(clientId, clientConfig, simpleConfigDef());
        triggerReport();
        assertExpectedShapeOfSubmittedEvents(clientId, clientConfig, "KafkaProducer");
    }

    @Test
    public void consumerEventsHaveExpectedShape() {
        config = makeConfigWithSslAndSasl();
        final String clientId = "consumer";
        final ConsumerConfig clientConfig = consumerConfig();
        emitter.registerConfiguration(clientId, clientConfig, simpleConfigDef());
        triggerReport();
        assertExpectedShapeOfSubmittedEvents(clientId, clientConfig, "KafkaConsumer");
    }

    @Test
    public void adminEventsHaveExpectedShape() {
        config = makeConfigWithSslAndSasl();
        final String clientId = "admin";
        final AdminClientConfig clientConfig = adminConfig();
        emitter.registerConfiguration(clientId, clientConfig, simpleConfigDef());
        triggerReport();
        assertExpectedShapeOfSubmittedEvents(clientId, clientConfig, "KafkaAdmin");
    }

    @Test
    public void unknownEventsHaveExpectedShape() {
        config = makeConfigWithSslAndSasl();
        final String clientId = "unknown";
        // pretend we've got some amazing new client!
        final AbstractConfig clientConfig = new MysteryAbstractConfig();
        emitter.registerConfiguration(clientId, clientConfig, simpleConfigDef());
        triggerReport();
        assertExpectedShapeOfSubmittedEvents(clientId, clientConfig, "KafkaUnknownClient");
    }

    /**
     * asserts that {@link #submittedEvents} contains general, SSL and SASL events corresponding to a given
     * client's config and that the expected attributes are included in the events.
     * @param clientId the client id
     * @param clientConfig config
     * @param eventTypePrefix prefix of the event type
     */
    private void assertExpectedShapeOfSubmittedEvents(final String clientId, final AbstractConfig clientConfig, final String eventTypePrefix) {
        assertThat(submittedEvents, hasSize(4));

        final ConfigTestUtil.SubmittedEvent generalEvent = findSubmittedEvent(eventTypePrefix + "Config", clientId);
        assertThat(generalEvent, notNullValue());
        assertExpectedNonConfigAttributesCorrectness(clientId, generalEvent);
        assertExpectedPropertiesInEventAttributesPresence(generalEvent, clientConfig, ConfigScope.GENERAL);

        final ConfigTestUtil.SubmittedEvent defaultsEvent =
                findSubmittedEvent(eventTypePrefix + "OverriddenDefaultConfig", clientId);
        assertThat(defaultsEvent, notNullValue());
        assertExpectedNonConfigAttributesCorrectness(clientId, defaultsEvent);
        assertExpectedPropertiesInEventAttributesPresence(defaultsEvent, clientConfig, ConfigScope.GENERAL_OVERRIDDEN_DEFAULTS);

        final ConfigTestUtil.SubmittedEvent sslEvent = findSubmittedEvent(eventTypePrefix + "SslConfig", clientId);
        assertThat(sslEvent, notNullValue());
        assertExpectedNonConfigAttributesCorrectness(clientId, sslEvent);
        assertExpectedPropertiesInEventAttributesPresence(sslEvent, clientConfig, ConfigScope.SSL);

        final ConfigTestUtil.SubmittedEvent saslEvent = findSubmittedEvent(eventTypePrefix + "SaslConfig", clientId);
        assertThat(saslEvent, notNullValue());
        assertExpectedNonConfigAttributesCorrectness(clientId, saslEvent);
        assertExpectedPropertiesInEventAttributesPresence(saslEvent, clientConfig, ConfigScope.SASL);
    }

    private void assertExpectedNonConfigAttributesCorrectness(final String clientId, final ConfigTestUtil.SubmittedEvent generalEvent) {
        assertThat(generalEvent.attributes.get("clientId"), equalTo(clientId));
        assertThat(generalEvent.attributes.get("kafka.version"), equalTo(AppInfoParser.getVersion()));
    }

    private void assertExpectedPropertiesInEventAttributesPresence(
            final ConfigTestUtil.SubmittedEvent event,
            final AbstractConfig clientConfig,
            final ConfigScope scope) {

        final Supplier<Stream<String>> dePrefixedEventConfigAttrNames = () -> event.attributes.keySet().stream()
                .filter(k -> k.startsWith("c_") || k.startsWith("d_")).map(k -> k.substring(2));

        // verify ownership is as expected
        Set<String> attrNames = dePrefixedEventConfigAttrNames.get().collect(Collectors.toSet());
        switch (scope) {
            case GENERAL:
            case GENERAL_OVERRIDDEN_DEFAULTS:
                long count = attrNames.stream().filter(s -> s.startsWith("ssl.") || s.startsWith("sasl.")).count();
                assertThat(count, equalTo(0L));
                break;
            case SSL:
                assertThat(attrNames.stream().filter(s -> !s.startsWith("ssl.")).count(), equalTo(0L));
                break;
            case SASL:
                assertThat(attrNames.stream().filter(s -> !s.startsWith("sasl.")).count(), equalTo(0L));
                break;
        }

        final String[] expectedConfigAttrs;
        if (scope.isOverriddenDefault()) {
            if (scope == ConfigScope.GENERAL_OVERRIDDEN_DEFAULTS && !event.eventType.contains("Unknown")) {
                // the only default established via simpleConfigDef(), which regular clients
                // should have values for that appear to be overridden
                expectedConfigAttrs = new String[] {SOME_REAL_SHARED_CONFIG_WITH_DEFAULT_VALUE_KEY};
            } else {
                // won't be an owned property for other defiance scopes, if there are any
                expectedConfigAttrs = new String[0];
            }
        } else {
            expectedConfigAttrs = clientConfig.values().entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getKey().equals(CommonClientConfigs.CLIENT_ID_CONFIG) && scope.ownsProperty(e.getKey()))
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new);
        }

        assertThat(attrNames, containsInAnyOrder(expectedConfigAttrs));
        assertThat(attrNames.size(), equalTo(expectedConfigAttrs.length));
    }

    @Test
    public void classPropertiesAreSerializedToClassNames() {
        config = makeStandardConfigWithoutOverriddenDefaultReporting();
        final String clientId = "producer";
        // specifying the classname as a property...
        final ProducerConfig clientConfig = producerConfig(
                ProducerConfig.PARTITIONER_CLASS_CONFIG, SomePartitioner.class.getName());
        // ... will lead to the actual class in the config values ...
        assertThat(clientConfig.values().get(ProducerConfig.PARTITIONER_CLASS_CONFIG), equalTo(SomePartitioner.class));
        emitter.registerConfiguration(clientId, clientConfig, simpleConfigDef());
        triggerReport();
        assertThat(submittedEvents, hasSize(1));
        // ... which we should then report as the class name again (i.e., no default toString() is used)
        assertThat(submittedEvents.get(0)
                .hasAttribute(configValueAttrKey(ProducerConfig.PARTITIONER_CLASS_CONFIG), SomePartitioner.class.getName()),
                is(true));
    }

    private static String configValueAttrKey(String propertyName) {
        return ConfigScope.GENERAL.configEventAttrName(propertyName);
    }

    private static String overriddenDefaultValueAttrKey(String propertyName) {
        return ConfigScope.GENERAL_OVERRIDDEN_DEFAULTS.configEventAttrName(propertyName);
    }

    @Test
    public void listsAreSerializedToStringRepresentations() {
        config = makeStandardConfigWithoutOverriddenDefaultReporting();
        final String clientId = "producer";
        // specifying the classname as a property...
        final ProducerConfig clientConfig = producerConfig(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "server1:1,server2:2,server3:3");
        // ... will lead to a list representation in the config values ...
        assertThat(clientConfig.values().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG),
                equalTo(Arrays.asList("server1:1", "server2:2", "server3:3")));
        emitter.registerConfiguration(clientId, clientConfig, simpleConfigDef());
        triggerReport();
        assertThat(submittedEvents, hasSize(1));
        // ... which we'll turn into a list format that's somewhat suited to nrqling
        assertThat(submittedEvents.get(0)
                .hasAttribute(configValueAttrKey(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG), "|server1:1|server2:2|server3:3|"),
                is(true));
    }

    @Test
    public void constructionsCountIsUpdated() {
        config = makeStandardConfigWithoutOverriddenDefaultReporting();

        emitter.registerConfiguration("producer", producerConfig(), simpleConfigDef());
        triggerReport();
        assertThat(submittedEvents, hasSize(1));
        assertThat(submittedEvents.get(0).attributes.get("constructions"), equalTo(1));

        emitter.registerConfiguration("producer", producerConfig(), simpleConfigDef());
        triggerReport();
        assertThat(submittedEvents, hasSize(2));
        assertThat(submittedEvents.get(1).attributes.get("constructions"), equalTo(2));
    }

    @Test
    public void producerOverriddenDefaultReported() {
        config = makeStandardConfig();

        emitter.registerConfiguration("producer", producerConfig(), simpleConfigDef());
        triggerReport();
        assertThat(submittedEvents, hasSize(2));
        final ConfigTestUtil.SubmittedEvent event = 
                findSubmittedEvent("KafkaProducerOverriddenDefaultConfig", "producer");
        final String defaultValueKey = overriddenDefaultValueAttrKey(SOME_REAL_SHARED_CONFIG_WITH_DEFAULT_VALUE_KEY);
        assertThat(event.attributes.keySet(),
                containsInAnyOrder("kafka.version", "clientId", "constructions", defaultValueKey));
        assertThat(event.attributes.size(), equalTo(4));
        assertThat(event.attributes.get(defaultValueKey), equalTo(SOME_REAL_SHARED_CONFIG_DEFAULT_VALUE));
    }

    @Test
    public void producerDefaultNotReportedAsOverride() {
        config = makeStandardConfig();
        
        // if we configure the producer to explicitly have the default value reported by simpleConfigDef()
        // (this is not normally the case since it's grabbing the REAL Kafka default), then no override should
        // be reported
        emitter.registerConfiguration(
                "producer",
                producerConfig(SOME_REAL_SHARED_CONFIG_WITH_DEFAULT_VALUE_KEY, SOME_REAL_SHARED_CONFIG_DEFAULT_VALUE),
                simpleConfigDef());
        triggerReport();
        assertThat(submittedEvents, hasSize(2));
        // the event will still be emitted, but without the attribute
        final ConfigTestUtil.SubmittedEvent event = 
                findSubmittedEvent("KafkaProducerOverriddenDefaultConfig", "producer");
        assertThat(event.attributes.keySet(),
                containsInAnyOrder("kafka.version", "clientId", "constructions"));
        assertThat(event.attributes.size(), equalTo(3));
    }

    @Test
    public void envVarIsReportedWithConfigVer() {
        config = makeStandardConfigWithoutOverriddenDefaultReporting();
        envVars.put("DEPLOYED_VERSION", "release-42");
        envVars.put("COMMIT_REF", "cafedead");
        envVars.put("IGNORED", "dont panic");

        emitter.registerConfiguration("producer", producerConfig(), simpleConfigDef());
        triggerReport();

        assertEquals(1, submittedEvents.size());
        final ConfigTestUtil.SubmittedEvent event = submittedEvents.get(0);
        assertEquals("release-42", event.attributes.get("DEPLOYED_VERSION"));
        assertEquals("cafedead", event.attributes.get("COMMIT_REF"));
    }

    private ConfigTestUtil.SubmittedEvent findSubmittedEvent(final String eventType, final String clientId) {
        return submittedEvents.stream()
                .filter(e -> e.isClientConfig(eventType, clientId))
                .findFirst()
                .orElse(null);
    }

    private void triggerReport() {
        emitter.reportConfigs();
    }

    public static class SomePartitioner implements Partitioner {
        @Override
        public int partition(final String topic, final Object key, final byte[] keyBytes, final Object value, final byte[] valueBytes, final Cluster cluster) {
            return 0;
        }

        @Override
        public void close() {}

        @Override
        public void configure(final Map<String, ?> configs) {}
    }

    private ProducerConfig producerConfig(final Object... additionalConfigs) {
        final Map<String, Object> props = new HashMap<>();
        props.put("key.serializer", ByteArraySerializer.class.getName());
        props.put("value.serializer", ByteArraySerializer.class.getName());
        fillAdditionalConfig(props, additionalConfigs);
        return new ProducerConfig(props);
    }

    private void fillAdditionalConfig(final Map<String, Object> props, final Object[] additionalConfigs) {
        assertThat(additionalConfigs.length % 2, equalTo(0));
        for (int i = 0; i < additionalConfigs.length; i += 2) {
            props.put((String) additionalConfigs[i], additionalConfigs[i + 1]);
        }
    }

    private ConsumerConfig consumerConfig(final Object... additionalConfigs) {
        final Map<String, Object> props = new HashMap<>();
        props.put("key.deserializer", ByteArrayDeserializer.class.getName());
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        fillAdditionalConfig(props, additionalConfigs);
        return new ConsumerConfig(props);
    }

    private AdminClientConfig adminConfig(final Object... additionalConfigs) {
        final Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", "blundstones:1234");
        fillAdditionalConfig(props, additionalConfigs);
        return new AdminClientConfig(props);
    }

    private ConfigEmissionConfiguration makeStandardConfig() {
        return new ConfigEmissionConfiguration(true, true, false, false, DELAY, FREQUENCY, CLIENT_COUNT_CAP);
    }
    
    private ConfigEmissionConfiguration makeStandardConfigWithoutOverriddenDefaultReporting() {
        return new ConfigEmissionConfiguration(true, false, false, false, DELAY, FREQUENCY, CLIENT_COUNT_CAP);
    }

    private ConfigEmissionConfiguration makeConfigWithSslAndSasl() {
        return new ConfigEmissionConfiguration(true, true, true, true, DELAY, FREQUENCY, CLIENT_COUNT_CAP);
    }
    
    private static class MysteryAbstractConfig extends AbstractConfig {
        public MysteryAbstractConfig() {
            super(new ConfigDef()
                    .define("boop",
                            ConfigDef.Type.BOOLEAN,
                            true,
                            ConfigDef.Importance.LOW,
                            "BOOP!")
                    .withClientSaslSupport()
                    .withClientSslSupport(), Collections.emptyMap());
        }
    }

    private class TestableConfigEmitter extends ConfigEmitter {

        @Override
        protected ConfigEmissionConfiguration readConfiguration() {
            return config;
        }

        @Override
        protected Agent getAgent() {
            return agent;
        }

        @Override
        protected void scheduleEmissions() {
            // avoid creating the scheduler threads by never scheduling emissions -- we'll force
            // them to happen manually
        }

        @Override
        protected String readFromEnvironment(String name) {
            return envVars.get(name);
        }
    }
}
