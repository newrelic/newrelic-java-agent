package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.MockServiceManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class SamplerTest {

    private MockServiceManager serviceManager;

    @Before
    public void setup(){
        serviceManager = new MockServiceManager();
    }

    @Test
    public void testGetSamplerForType(){
        Sampler alwaysOnSampler = Sampler.getSamplerForType(Sampler.ALWAYS_ON);
        assertEquals(Sampler.ALWAYS_ON, alwaysOnSampler.getType());

        Sampler alwaysOffSampler = Sampler.getSamplerForType(Sampler.ALWAYS_OFF);
        assertEquals(Sampler.ALWAYS_OFF, alwaysOffSampler.getType());

        //these all should mean the same thing and retrieve an adaptive sampler.
        List<Sampler> adaptiveSamplers = new ArrayList<>();
        Sampler adaptiveSampler = Sampler.getSamplerForType(Sampler.ADAPTIVE);
        Sampler defaultSampler = Sampler.getSamplerForType("default");
        Sampler gibberishSampler = Sampler.getSamplerForType("madeUpType");
        Sampler noSampler = Sampler.getSamplerForType("");
        adaptiveSamplers.add(adaptiveSampler);
        adaptiveSamplers.add(defaultSampler);
        adaptiveSamplers.add(gibberishSampler);
        adaptiveSamplers.add(noSampler);

        for (Sampler sampler: adaptiveSamplers) {
            assertEquals(Sampler.ADAPTIVE, sampler.getType());
        }

    }

}