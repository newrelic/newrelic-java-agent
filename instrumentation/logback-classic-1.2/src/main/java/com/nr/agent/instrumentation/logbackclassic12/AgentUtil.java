package com.nr.agent.instrumentation.logbackclassic12;

import com.newrelic.api.agent.NewRelic;

import java.util.Map;

public class AgentUtil {

    public static Map<String, String> getLinkingMetadataAsMap() {
        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
        // TODO omit attributes that don't have values
        //  {trace.id=, hostname=192.168.1.8, entity.type=SERVICE, entity.guid=MjIxMjg2NHxBUE18QVBQTElDQVRJT058MTY2NjIxNDQ3NQ, entity.name=SpringBoot PetClinic, span.id=}
        return linkingMetadata;
    }

    public static String getLinkingMetadataAsString() {
        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
        // TODO omit attributes that don't have values
        //  {trace.id=, hostname=192.168.1.8, entity.type=SERVICE, entity.guid=MjIxMjg2NHxBUE18QVBQTElDQVRJT058MTY2NjIxNDQ3NQ, entity.name=SpringBoot PetClinic, span.id=}
        return linkingMetadata.toString();
    }
}
