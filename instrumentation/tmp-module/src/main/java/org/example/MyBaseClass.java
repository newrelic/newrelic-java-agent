package org.example;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass)
public abstract class MyBaseClass {

    public Object doNothing(Object in) {
        com.newrelic.api.agent.NewRelic.getAgent().getLogger().log(java.util.logging.Level.INFO, "Found doNothing!");
        return Weaver.callOriginal();
    }
}