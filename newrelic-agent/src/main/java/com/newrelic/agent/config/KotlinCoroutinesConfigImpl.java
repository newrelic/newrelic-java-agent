package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class KotlinCoroutinesConfigImpl extends BaseConfig implements KotlinCoroutinesConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.kotlin.coroutines.";
    public static final String CONTINUATIONS_ROOT = "continuations";
    public static final String SCOPES_ROOT = "scopes";
    public static final String DISPATCHED_ROOT = "dispatched";
    public static final String IGNORE = "ignore";
    public static final String DELAYED_ROOT = "delayed";
    public static final String ENABLED = "enabled";
    private static final boolean DELAY_DEFAULT = true;
    private String[] ignoredContinuations = null;
    private String[] ignoredScopes = null;
    private String[] ignoredDispatched = null;
    private boolean delayedEnabled = true;

    public KotlinCoroutinesConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        Map<String,String> continuations_root = getProperty(CONTINUATIONS_ROOT);

        if (continuations_root != null) {
            String continuationsToIgnore = continuations_root.get(IGNORE);
            ignoredContinuations = continuationsToIgnore == null ? new String[0] : continuationsToIgnore.split(",");
        } else {
            ignoredContinuations = new String[0];
        }

        Map<String, String> scopes_root = getProperty(SCOPES_ROOT);
        if (scopes_root != null) {
            String scopesToIgnore = scopes_root.get(IGNORE);
            ignoredScopes = scopesToIgnore == null ? new String[0] : scopesToIgnore.split(",");
        } else {
            ignoredScopes = new String[0];
        }

        Map<String, String> dispatched_root = getProperty(DISPATCHED_ROOT);
        if (dispatched_root != null) {
            String dispatchedToIgnore = dispatched_root.get(IGNORE);
            ignoredDispatched = dispatchedToIgnore == null ? new String[0] : dispatchedToIgnore.split(",");
        } else {
            ignoredDispatched = new String[0];
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
    public String[] getIgnoredScopes() {
        return ignoredScopes;
    }

    @Override
    public String[] getIgnoredDispatched() {
        return ignoredDispatched;
    }

    @Override
    public boolean isDelayedEnabled() {
        return delayedEnabled;
    }

}
