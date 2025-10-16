package com.newrelic.agent.tracing.samplers;

public interface Sampler {
    //static members
    String ADAPTIVE = "adaptive";
    String ALWAYS_OFF = "always_off";
    String ALWAYS_ON = "always_on";

    static Sampler getSamplerForType(String samplerType){
        Sampler sampler;
        switch (samplerType){
            case ALWAYS_OFF:
                sampler = new AlwaysOffSampler();
                break;
            case ALWAYS_ON:
                sampler = new AlwaysOnSampler();
                break;
            default:
                sampler = AdaptiveSampler.getSharedInstance();
        }
        return sampler;
    }

    //instance methods
    float calculatePriority();
    String getType();
}
