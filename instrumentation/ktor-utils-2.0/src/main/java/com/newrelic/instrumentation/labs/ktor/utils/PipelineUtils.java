package com.newrelic.instrumentation.labs.ktor.utils;

import java.util.HashSet;
import java.util.Set;

public class PipelineUtils {

    private static final Set<String> PIPELINES_TO_TRACK = new HashSet<String>();

    static {
        PIPELINES_TO_TRACK.add("HttpResponsePipeline");
    }


    public static boolean tracePipeline(String pipelineName) {
        return PIPELINES_TO_TRACK.contains(pipelineName);
    }

}
