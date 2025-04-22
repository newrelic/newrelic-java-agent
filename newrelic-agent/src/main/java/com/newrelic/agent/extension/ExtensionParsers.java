/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


import com.newrelic.agent.extension.dom.ExtensionDomParser;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;

public class ExtensionParsers {
    private final ExtensionParser yamlParser;
    private final ExtensionParser xmlParser;

    public ExtensionParsers(final List<ConfigurationConstruct> constructs) {
        SafeConstructor constructor = new SafeConstructor(new LoaderOptions()) {
            {
                for (ConfigurationConstruct construct : constructs) {
                    this.yamlConstructors.put(new Tag(construct.getName()), construct);
                }
            }
        };
        final Yaml yaml = new Yaml(constructor);

        yamlParser = new ExtensionParser() {

            @Override
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                Object config = yaml.load(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
                if (config instanceof Map) {
                    return new YamlExtension(classloader, (Map<String, Object>) config, custom);
                } else {
                    throw new Exception("Invalid yaml extension");
                }
            }

        };

        xmlParser = new ExtensionParser() {

            @Override
            public Extension parse(ClassLoader classloader, InputStream inputStream, boolean custom) throws Exception {
                com.newrelic.agent.extension.beans.Extension ext = ExtensionDomParser.readFile(inputStream);
                return new XmlExtension(getClass().getClassLoader(), ext.getName(), ext, custom);
            }

        };
    }

    public ExtensionParser getParser(String fileName) {
        if (fileName.endsWith(".yml")) {
            return yamlParser;
        } else {
            return xmlParser;
        }
    }

    public ExtensionParser getXmlParser() {
        return xmlParser;
    }

    public ExtensionParser getYamlParser() {
        return yamlParser;
    }

    public interface ExtensionParser {
        Extension parse(ClassLoader classLoader, InputStream stream, boolean custom) throws Exception;
    }
}
