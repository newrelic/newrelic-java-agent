package com.newrelic.agent.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KotlinCoroutinesConfigImpl extends BaseConfig implements KotlinCoroutinesConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.kotlin.coroutines.";
    public static final String CONTINUATIONS_ROOT = "continuations";
    public static final String SCOPES_ROOT = "scopes";
    public static final String DISPATCHED_ROOT = "dispatched";
    public static final String IGNORE = "ignore";
    public static final String IGNORE_REGEX = "ignoreRegex";
    public static final String DELAYED_ROOT = "delayed";
    public static final String ENABLED = "enabled";
    private static final boolean DELAY_DEFAULT = true;
    private String[] ignoredContinuations = null;
    private String[] ignoredScopes = null;
    private String[] ignoredDispatched = null;
    private String[] ignoredRegexContinuations = null;
    private String[] ignoredRegexScopes = null;
    private String[] ignoredRegexDispatched = null;
    private boolean delayedEnabled = true;

    public KotlinCoroutinesConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        Map<String,String> continuations_root = getProperty(CONTINUATIONS_ROOT);

        if (continuations_root != null) {
            String continuationsToIgnore = continuations_root.get(IGNORE);
            ignoredContinuations = splitString(continuationsToIgnore); //continuationsToIgnore == null ? new String[0] : continuationsToIgnore.split(",");
            String continuationsToIgnoreRegex = continuations_root.get(IGNORE_REGEX);
            ignoredRegexContinuations = splitString(continuationsToIgnoreRegex); //continuationsToIgnoreRegex == null ? new String[0] : continuationsToIgnoreRegex.split(",");
        } else {
            ignoredContinuations = new String[0];
            ignoredRegexContinuations = new String[0];
        }

        Map<String, String> scopes_root = getProperty(SCOPES_ROOT);
        if (scopes_root != null) {
            String scopesToIgnore = scopes_root.get(IGNORE);
            ignoredScopes = splitString(scopesToIgnore); //scopesToIgnore == null ? new String[0] : scopesToIgnore.split(",");
            String scopesToIgnoreRegex = scopes_root.get(IGNORE_REGEX);
            ignoredRegexScopes = splitString(scopesToIgnoreRegex); //scopesToIgnoreRegex == null ? new String[0] : scopesToIgnoreRegex.split(",");
        } else {
            ignoredScopes = new String[0];
            ignoredRegexScopes = new String[0];
        }

        Map<String, String> dispatched_root = getProperty(DISPATCHED_ROOT);
        if (dispatched_root != null) {
            String dispatchedToIgnore = dispatched_root.get(IGNORE);
            ignoredDispatched = splitString(dispatchedToIgnore);  //dispatchedToIgnore == null ? new String[0] : dispatchedToIgnore.split(",");
            String dispatchedToIgnoreRegex = dispatched_root.get(IGNORE_REGEX);
            ignoredRegexDispatched = splitString(dispatchedToIgnoreRegex); //dispatchedToIgnoreRegex == null ? new String[0] : dispatchedToIgnoreRegex.split(",");
        } else {
            ignoredDispatched = new String[0];
            ignoredRegexDispatched = new String[0];
        }

        Map<String, Object> delayed_root = getProperty(DELAYED_ROOT);
        if (delayed_root != null) {
            Object delayedObj = delayed_root.get(ENABLED);
            if(delayedObj != null) {
                if(delayedObj instanceof Boolean) {
                    delayedEnabled = (Boolean) delayedObj;
                } else if (delayedObj instanceof String) {
                    delayedEnabled = Boolean.parseBoolean((String) delayedObj);
                }
            }
        } else {
            delayedEnabled = DELAY_DEFAULT;
        }
    }

    static KotlinCoroutinesConfigImpl create(Map<String, Object> settings) {
        if(settings == null) {
            settings = Collections.emptyMap();
        }
        return new KotlinCoroutinesConfigImpl(settings);
    }

    @Override
    public String[] getIgnoredContinuations() {
        return ignoredContinuations;
    }

    @Override
    public String[] getIgnoredRegExContinuations() {
        return ignoredRegexContinuations;
    }

    @Override
    public String[] getIgnoredScopes() {
        return ignoredScopes;
    }

    @Override
    public String[] getIgnoredRegexScopes() {
        return ignoredRegexScopes;
    }

    @Override
    public String[] getIgnoredDispatched() {
        return ignoredDispatched;
    }

    @Override
    public String[] getIgnoredRegexDispatched() {
        return ignoredRegexDispatched;
    }

    @Override
    public boolean isDelayedEnabled() {
        return delayedEnabled;
    }

    private String[] splitString(String input) {
        if (input == null) {
            return new String[0];
        }
        String[] firstSplit = input.split("\"");
        List<String> result = new ArrayList<>();
        for (String s : firstSplit) {
            if(!s.trim().equals(",")) {
                result.add(s.trim());
            }
        }
        String[] returnValue = new String[result.size()];
        result.toArray(returnValue);
        return returnValue;
    }
}
