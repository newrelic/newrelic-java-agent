/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.logging.IAgentLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.newrelic.agent.config.SecurityAgentConfig.SECURITY_AGENT_CLASS_TRANSFORMER_EXCLUDES_TO_IGNORE;
import static com.newrelic.agent.config.SecurityAgentConfig.shouldInitializeSecurityAgent;

/**
 * This filter is used to skip certain classes during the classloading transform callback based on the class name.
 */
public class ClassNameFilter {
    private static final String EXCLUDES_FILE = "/META-INF/excludes";
    private final List<Pattern> excludePatterns = new LinkedList<>();
    private final List<Pattern> includePatterns = new LinkedList<>();
    private volatile Set<String> includeClasses = new HashSet<>();
    private final IAgentLogger logger;

    public ClassNameFilter(IAgentLogger logger) {
        this.logger = logger;
    }

    /**
     * Check if a class should be excluded from class transformation based on an excludes rule.
     * <p>
     * excludePatterns is a list of exclude rules merged together from user config and the default META-INF/excludes file.
     * Exclude rules are evaluated when determining whether to transform weaved classes in the InstrumentationContextManager
     * or pointcuts in the PointCutClassTransformer.
     *
     * @param className name of the class to check exclusion rule for
     * @return <code>true</code> if this is an excluded class, <code>false</code> if not
     */
    public boolean isExcluded(String className) {
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is the class included.
     *
     * @return <code>true</code> if this is an included class, <code>false</code> if not
     */
    public boolean isIncluded(String className) {
        if (includeClasses.contains(className)) {
            return true;
        }
        for (Pattern pattern : includePatterns) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add include/exclude classes in the configuration file.
     */
    public void addConfigClassFilters(AgentConfig config) {
        Set<String> excludes = config.getClassTransformerConfig().getExcludes();
        if (excludes.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exclude class name filters:");
            for (String exclude : excludes) {
                sb.append("\n").append(exclude);
                addExclude(exclude);
                if (SECURITY_AGENT_CLASS_TRANSFORMER_EXCLUDES_TO_IGNORE.contains(exclude)) {
                    logger.finer(exclude + " class_transformer exclude explicitly added by user config. The user configured exclude rule will" +
                            " take precedence and will not be ignored due to the security agent being enabled.");
                }
            }
            logger.finer(sb.toString());
        }
        Set<String> includes = config.getClassTransformerConfig().getIncludes();
        for (String include : includes) {
            addInclude(include);
        }
    }

    /**
     * Add excluded classes in the META-INF/excludes file.
     */
    public void addExcludeFileClassFilters() {
        InputStream iStream = this.getClass().getResourceAsStream(EXCLUDES_FILE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        List<String> excludeList = new LinkedList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    excludeList.add(line);
                }
            }
        } catch (IOException ex) {
            logger.severe(MessageFormat.format(
                    "Unable to read the class excludes file at {0} found within the New Relic jar.", EXCLUDES_FILE));
        } finally {
            try {
                iStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        boolean ignoreExcludeForSecurityAgent = false;
        String formattedIgnoredExcludes = String.join(",", SECURITY_AGENT_CLASS_TRANSFORMER_EXCLUDES_TO_IGNORE);
        boolean shouldInitializeSecurityAgent = shouldInitializeSecurityAgent();

        for (String exclude : excludeList) {
            ignoreExcludeForSecurityAgent = shouldInitializeSecurityAgent && SECURITY_AGENT_CLASS_TRANSFORMER_EXCLUDES_TO_IGNORE.contains(exclude);
            if (ignoreExcludeForSecurityAgent) {
                logger.finer("Ignored " + exclude + " class_transformer exclude defined in META-INF/excludes because the security agent is enabled. " +
                        "This can be overridden by explicitly setting newrelic.config.class_transformer.excludes=" + formattedIgnoredExcludes +
                        " or NEW_RELIC_CLASS_TRANSFORMER_EXCLUDES=" + formattedIgnoredExcludes);
            } else {
                addExclude(exclude);
            }
        }

        if (ignoreExcludeForSecurityAgent) {
            for (String exclude : SECURITY_AGENT_CLASS_TRANSFORMER_EXCLUDES_TO_IGNORE) {
                // Remove the default exclude rule if the security agent is enabled. If a user explicitly adds one of these exclude rules via agent config
                // then it will be added back to the list via ClassNameFilter.addConfigClassFilters, effectively taking precedence over this removal logic.
                excludeList.remove(exclude);
            }
        }
        logger.finer("Excludes initialized: " + excludeList);
    }

    /**
     * Include matching classes.
     *
     * @param include either a class name or a regular expression
     */
    public void addInclude(String include) {
        if (isRegex(include)) {
            addIncludeRegex(include);
        } else {
            addIncludeClass(include);
        }
    }

    /**
     * Include the given class.
     *
     * @param className the name of the class to include
     */
    public void addIncludeClass(String className) {
        String regex = classNameToRegex(className);
        addIncludeRegex(regex);
    }

    /**
     * Include classes matching the regular expression.
     *
     * @param regex a regular expression matching classes to include
     */
    public void addIncludeRegex(String regex) {
        Pattern pattern = regexToPattern(regex);
        if (pattern != null) {
            includePatterns.add(pattern);
        }
    }

    /**
     * Exclude matching classes.
     *
     * @param exclude either a class name or a regular expression
     */
    public void addExclude(String exclude) {
        if (isRegex(exclude)) {
            addExcludeRegex(exclude);
        } else {
            addExcludeClass(exclude);
        }
    }

    /**
     * Exclude the given class.
     *
     * @param className the name of the class to exclude
     */
    public void addExcludeClass(String className) {
        String regex = classNameToRegex(className);
        addExcludeRegex(regex);
    }

    /**
     * Exclude classes matching the regular expression.
     *
     * @param regex a regular expression matching classes to include
     */
    public void addExcludeRegex(String regex) {
        Pattern pattern = regexToPattern(regex);
        if (pattern != null) {
            excludePatterns.add(pattern);
        }
    }

    /**
     * Create a regular expression for a class.
     *
     * @return a regular expression that matches only the given class
     */
    private String classNameToRegex(String className) {
        return "^" + className.replace("$", "\\$") + "$";
    }

    /**
     * Compile a regular expression.
     *
     * @return a pattern or null if the regular expression could not be compiled
     */
    private Pattern regexToPattern(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (Exception e) {
            logger.severe(MessageFormat.format("Unable to compile pattern: {0}", regex));
        }
        return null;

    }

    /**
     * Is the argument a regular expression (rather than a class name).
     *
     * @param value
     * @return <code>true</code> if this is a regular expression, <code>false</code> if not
     */
    private boolean isRegex(String value) {
        return value.indexOf('*') >= 0 || value.indexOf('|') >= 0 || value.indexOf('^') >= 0;
    }

    public void addClassMatcherIncludes(Collection<ClassMatcher> classMatchers) {
        Set<String> classNames = new HashSet<>();
        classNames.addAll(includeClasses);

        for (ClassMatcher classMatcher : classMatchers) {
            for (String className : classMatcher.getClassNames()) {
                classNames.add(className);
            }
        }
        logger.finer("Class name inclusions: " + classNames);
        includeClasses = classNames;
    }

}
