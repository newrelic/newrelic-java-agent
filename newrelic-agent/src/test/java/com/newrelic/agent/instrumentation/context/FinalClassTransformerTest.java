package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.commons.Method;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;
import java.util.HashSet;

public class FinalClassTransformerTest {

    @Before
    public void setup() {
        MockServiceManager serviceManager = new MockServiceManager();  // yes, this is necessary
    }

    @Test
    public void test_transform_invalidClassName() throws IllegalClassFormatException {
        FinalClassTransformer target = new FinalClassTransformer();
        byte[] result = target.transform(this.getClass().getClassLoader(), null, FinalClassTransformerTest.class,
                null, null, null, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_transform_exception() throws IllegalClassFormatException {
        FinalClassTransformer target = new FinalClassTransformer();
        byte[] result = target.transform(this.getClass().getClassLoader(), "invalid", FinalClassTransformerTest.class,
                null, null, null, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_transform_withInstrTypesAndName() throws Exception {
        test_transform(true, "test_transform_withInstrTypesAndName");
    }

    @Test
    public void test_transform_withoutInstrTypesAndName() throws Exception {
        test_transform(false, "test_transform_withoutInstrTypesAndName");
    }

    private void test_transform(boolean setTypesAndNames, String methodName) throws Exception {
        FinalClassTransformer target = new FinalClassTransformer();
        InstrumentationContext context = Mockito.mock(InstrumentationContext.class);
        Mockito.when(context.isModified(Mockito.any())).thenReturn(true);
        Method aMethod = new Method(methodName, "()V");
        Mockito.when(context.getTimedMethods()).thenReturn(new HashSet<>(Arrays.asList(aMethod)));
        Mockito.when(context.isUsingLegacyInstrumentation()).thenReturn(true);
        Mockito.when(context.hasModifiedClassStructure()).thenReturn(true);

        TraceInformation traceInformation = new TraceInformation();
        TraceDetailsBuilder builder = TraceDetailsBuilder.newBuilder().setCustom(true);
        if (setTypesAndNames) {
            Mockito.when(context.getOldStylePointCut(Mockito.any())).thenReturn(Mockito.mock(PointCut.class));
            Mockito.when(context.getMergeInstrumentationPackages(Mockito.any())).thenReturn(Arrays.asList("package1"));
            builder = builder
                    .setInstrumentationType(InstrumentationType.TraceAnnotation)
                    .setInstrumentationSourceName("sourceName");
        }
        TraceDetails traceDetails = builder.build();
        traceInformation.putTraceAnnotation(aMethod, traceDetails);
        Mockito.when(context.getTraceInformation()).thenReturn(traceInformation);

        byte[] classBytes = WeaveUtils.getClassBytesFromClassLoaderResource(FinalClassTransformerTest.class.getName(), FinalClassTransformerTest.class.getClassLoader());
        byte[] result = target.transform(this.getClass().getClassLoader(), this.getClass().getName(), FinalClassTransformerTest.class,
                null, classBytes, context, null);
        Assert.assertNotNull(result);
    }

}
