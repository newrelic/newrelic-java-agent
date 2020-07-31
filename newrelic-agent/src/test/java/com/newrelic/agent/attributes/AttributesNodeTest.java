/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.Sets;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttributesNodeTest {

    private AttributesNode createConfigNode(AgentConfig config) {

        List<String> defaultExclude = config.getAttributesConfig().attributesRootExclude();
        List<String> defaultInclude = config.getAttributesConfig().attributesRootInclude();
        Set<String> exclude = DestinationFilter.getExcluded(config, defaultExclude,
                AgentConfigImpl.BROWSER_MONITORING);
        Set<String> include = DestinationFilter.getIncluded(config, defaultInclude,
                AgentConfigImpl.BROWSER_MONITORING);

        return DefaultDestinationPredicate.generateConfigTrie("Test", exclude, include);
    }

    private AttributesNode createDefaultNode() {
        return DefaultDestinationPredicate.generateDefaultTrie("Test", Collections.<String> emptySet());
    }

    @Test
    public void defaultTrie() {
        AttributesNode root = DefaultDestinationPredicate.generateDefaultTrie("Test", Sets.newHashSet("request.*",
                "message.parameters.*", "jvm.*"));
        runAndVerifyOutput(root, "onlymatchroot", true);
        runAndVerifyOutput(root, "request", true);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.status", false);
        runAndVerifyOutput(root, "request.parameters.foo", false);
        runAndVerifyOutput(root, "messages.parameters.bar", true);
        runAndVerifyOutput(root, "message.parameters.bar", false);
        runAndVerifyOutput(root, "jvmtest", true);
        runAndVerifyOutput(root, "jvm.test", false);
        runAndVerifyOutput(root, "*", true);

        // turn everything off
        root = DefaultDestinationPredicate.generateDefaultTrie("Test", Sets.newHashSet("*"));
        runAndVerifyOutput(root, "onlymatchroot", false);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.status", false);
        runAndVerifyOutput(root, "request.parameters.foo", false);
        runAndVerifyOutput(root, "message.parameters.bar", false);
        runAndVerifyOutput(root, "jvmtest", false);
        runAndVerifyOutput(root, "jvm.test", false);
        runAndVerifyOutput(root, "*", false);

        // verify tree gets formed correctly
        root = DefaultDestinationPredicate.generateDefaultTrie("Test", Sets.newHashSet("band*", "bend*", "bat*", "ba*",
                "b*"));
        runAndVerifyOutput(root, "*", true);
        runAndVerifyOutput(root, "b", false);
        runAndVerifyOutput(root, "ba", false);
        runAndVerifyOutput(root, "batter", false);
        runAndVerifyOutput(root, "banner", false);
        runAndVerifyOutput(root, "be", false);
        runAndVerifyOutput(root, "bent", false);
    }

    @Test
    public void testGenerateTrieExcludesOne() {
        // general config
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("enabled", Boolean.TRUE);
        attSettings.put("exclude", "hello");
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        // matches the exclude hello node
        runAndVerifyOutput(root, "hello", false);
        // these only match the root node
        runAndVerifyOutput(root, "onlymatchroot", null);
        runAndVerifyOutput(root, "hel", null);

        root = createDefaultNode();
        runAndVerifyOutput(root, "onlymatchroot", true);
        runAndVerifyOutput(root, "hel", true);
    }

    @Test
    public void testGenerateTrieExcludesMultiple() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hello, good, bye, request.*");
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "hello", false);
        runAndVerifyOutput(root, "good", false);
        runAndVerifyOutput(root, "bye", false);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.params.foo", false);
        runAndVerifyOutput(root, "request.status", false);

        // these only match the root node - defaults
        runAndVerifyOutput(root, "onlymatchroot", null);
        runAndVerifyOutput(root, "request", null);

        root = createDefaultNode();
        runAndVerifyOutput(root, "onlymatchroot", true);
        runAndVerifyOutput(root, "request", true);
    }

    @Test
    public void testGenerateTrieIncludeOne() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("include", "hello");
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        // matches the exclude hello node
        runAndVerifyOutput(root, "hello", true);
        // these only match the root node
        runAndVerifyOutput(root, "onlymatchroot", null);
        runAndVerifyOutput(root, "hel", null);

        root = createDefaultNode();
        runAndVerifyOutput(root, "onlymatchroot", true);
        runAndVerifyOutput(root, "hel", true);
    }

    @Test
    public void testGenerateTrieIncludeMultiple() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("include", "hello, good, bye, request.*");
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "hello", true);
        runAndVerifyOutput(root, "good", true);
        runAndVerifyOutput(root, "bye", true);
        runAndVerifyOutput(root, "request.", true);
        runAndVerifyOutput(root, "request.params.foo", true);
        runAndVerifyOutput(root, "request.status", true);

        // these only match the root node - defaults
        runAndVerifyOutput(root, "onlymatchroot", null);
        runAndVerifyOutput(root, "request", null);

        root = createDefaultNode();
        runAndVerifyOutput(root, "onlymatchroot", true);
        runAndVerifyOutput(root, "request", true);
    }

    @Test
    public void testGenerateTrieMixIncludeExclude() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "hel", false);
        runAndVerifyOutput(root, "helloo", false);
        runAndVerifyOutput(root, "hello", true);
        runAndVerifyOutput(root, "good", false);
        runAndVerifyOutput(root, "goody", true);
        runAndVerifyOutput(root, "go", null);
        runAndVerifyOutput(root, "bye", true);
        runAndVerifyOutput(root, "byeee", false);
        runAndVerifyOutput(root, "request", false);
        runAndVerifyOutput(root, "request.", true);
        runAndVerifyOutput(root, "request.params.foo", true);
        runAndVerifyOutput(root, "request.status", true);

        runAndVerifyOutput(root, "onlymatchroot", null);

        root = createDefaultNode();
        runAndVerifyOutput(root, "onlymatchroot", true);
    }

    @Test
    public void testGenerateTrieOverrideToEnabled() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hel*, good, bye*, request");
        attSettings.put("include", "hello, good*, bye, request.*");
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "hel", false);
        runAndVerifyOutput(root, "helloo", false);
        runAndVerifyOutput(root, "hello", true);
        runAndVerifyOutput(root, "bye", true);
        runAndVerifyOutput(root, "byeee", false);
        runAndVerifyOutput(root, "request", false);
        runAndVerifyOutput(root, "request.", true);
        runAndVerifyOutput(root, "request.params.foo", true);
        runAndVerifyOutput(root, "request.status", true);
        runAndVerifyOutput(root, "onlymatchroot", null);
    }

    @Test
    public void testGenerateTrieOneSpecificInclude() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hello*, request*");
        attSettings.put("include", "hello, request.params.*");

        Map<String, Object> specific = new HashMap<>();
        specific.put("enabled", Boolean.TRUE);
        specific.put("include", "sammy");
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        browser.put("attributes", specific);
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "sammy", true);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.params.bar", true);
        runAndVerifyOutput(root, "onlymatchroot", null);
    }

    @Test
    public void testGenerateTrieOneSpecificExclude() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hello*, request*");
        attSettings.put("include", "hello, request.params.*");

        Map<String, Object> specific = new HashMap<>();
        specific.put("enabled", Boolean.TRUE);
        specific.put("exclude", "sammy");
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        browser.put("attributes", specific);
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "sammy", false);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.params.bar", true);
        runAndVerifyOutput(root, "onlymatchroot", null);

    }

    @Test
    public void testGenerateTrieOneSpecific() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hello*, request*");
        attSettings.put("include", "hello, request.params.*");

        Map<String, Object> specific = new HashMap<>();
        specific.put("enabled", Boolean.TRUE);
        specific.put("exclude", "request.params.foo");
        specific.put("include", "helloa");
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        browser.put("attributes", specific);
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "hel", null);
        runAndVerifyOutput(root, "helloo", false);
        runAndVerifyOutput(root, "hello", true);
        runAndVerifyOutput(root, "helloa", true);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.params.foo", false);
        runAndVerifyOutput(root, "request.params.bar", true);
        runAndVerifyOutput(root, "onlymatchroot", null);
    }

    @Test
    public void testGenerateTrieMixSpecific() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> attSettings = new HashMap<>();
        settings.put("attributes", attSettings);
        attSettings.put("exclude", "hello*, request*");
        attSettings.put("include", "hello, request.params.*");

        Map<String, Object> specific = new HashMap<>();
        specific.put("enabled", Boolean.TRUE);
        specific.put("exclude", "request.params.foo, request.params.bar, ba*");
        specific.put("include", "helloa, hellob, helloc, sa*");
        Map<String, Object> browser = new HashMap<>();
        settings.put("browser_monitoring", browser);
        browser.put("attributes", specific);
        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);

        AttributesNode root = createConfigNode(config);
        runAndVerifyOutput(root, "hel", null);
        runAndVerifyOutput(root, "helloo", false);
        runAndVerifyOutput(root, "hello", true);
        runAndVerifyOutput(root, "helloa", true);
        runAndVerifyOutput(root, "hellob", true);
        runAndVerifyOutput(root, "request.", false);
        runAndVerifyOutput(root, "request.params.foo", false);
        runAndVerifyOutput(root, "request.params.bar", false);
        runAndVerifyOutput(root, "request.params.sally", true);
        runAndVerifyOutput(root, "request.params.bobby", true);
        runAndVerifyOutput(root, "ba", false);
        runAndVerifyOutput(root, "sa", true);
        runAndVerifyOutput(root, "banner", false);
        runAndVerifyOutput(root, "sally", true);
        runAndVerifyOutput(root, "onlymatchroot", null);
    }

    private void runAndVerifyOutput(AttributesNode root, String input, Boolean expected) {
        if (expected == null) {
            Assert.assertNull("unexpected value for input " + input, root.applyRules(input));
        } else {
            Assert.assertEquals("unexpected value for input " + input, expected, root.applyRules(input));
        }
    }
}
