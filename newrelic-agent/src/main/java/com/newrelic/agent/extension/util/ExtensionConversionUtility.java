/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.ClassName;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.AllClassesMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.LambdaMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ReturnTypeMethodMatcher;
import com.newrelic.agent.instrumentation.tracing.ParameterAttributeName;
import org.objectweb.asm.Type;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Static methods for converting an xml file into a point cut.
 * 
 * @since Sep 18, 2012
 */
public final class ExtensionConversionUtility {

    /** The default directory where extension files can be placed. */
    public static final String DEFAULT_CONFIG_DIRECTORY = "extensions";

    /**
     * 
     * Creates this ExtensionConversion. This is a private method since this is a utility class and therefore should
     * never be instantiated.
     */
    private ExtensionConversionUtility() {
        super();
    }

    /**
     * Validates the extension attributes. Throws a runtime exception if one of the parameters is invalid.
     * 
     * @param extension The extension to be validated.
     * @throws XmlException
     */
    public static void validateExtensionAttributes(final Extension extension) throws XmlException {
        if (extension == null) {
            throw new XmlException(XmlParsingMessages.NO_EXT);
        } else if ((extension.getName() == null) || (extension.getName().length() == 0)) {
            throw new XmlException(XmlParsingMessages.NO_EXT_NAME);
        } else if (extension.getVersion() < 0) {
            throw new XmlException(XmlParsingMessages.NEG_EXT_VER);
        }
    }

    /**
     * Validates that the instrument is present and that there are point cuts in the instrumentation.
     * 
     * @param instrument The instrument.
     * @throws XmlException
     */
    private static void validateInstrument(final Instrumentation instrument) throws XmlException {
        if (instrument == null) {
            throw new XmlException(XmlParsingMessages.NO_INST_TAG);
        } else {
            List<Pointcut> pcs = instrument.getPointcut();
            if ((pcs == null) || pcs.isEmpty()) {
                throw new XmlException(XmlParsingMessages.NO_PC_TAGS);
            }
        }
    }

    /**
     * Should be used to validate the XML. No logging is performed with this method.
     * 
     * @param ext The extension read in.
     * @return The list of point cuts.
     * @throws XmlException
     */
    public static List<ExtensionClassAndMethodMatcher> convertToPointCutsForValidation(Extension ext)
            throws XmlException {
        List<ExtensionClassAndMethodMatcher> pointCutsOut = new ArrayList<>();
        Instrumentation inst = ext.getInstrumentation();
        validateExtensionAttributes(ext);
        // this mandates that an instrumentation element is present
        validateInstrument(inst);
        List<Pointcut> pcs = inst.getPointcut();

        String defaultMetricPrefix = createDefaultMetricPrefix(inst, true);

        Map<String, MethodMapper> classesToMethods = new HashMap<>();
        for (Pointcut pc : pcs) {
            pointCutsOut.add(createPointCut(ext, pc, defaultMetricPrefix, ext.getName(), classesToMethods, true,
                    InstrumentationType.LocalCustomXml, false));
        }

        return pointCutsOut;
    }

    /**
     * Takes in a collection of extensions and returns the converted point cuts. These methods also perform validation.
     * A run time exception will be thrown if the input extension is invalid.
     */
    public static List<ExtensionClassAndMethodMatcher> convertToEnabledPointCuts(
            final Collection<Extension> extensions, boolean custom, InstrumentationType type) {
        return convertToEnabledPointCuts(extensions, custom, type, true);
    }

    /**
     * Takes in a collection of extensions and returns the converted point cuts. These methods also perform validation.
     * A run time exception will be thrown if the input extension is invalid.
     */
    public static List<ExtensionClassAndMethodMatcher> convertToEnabledPointCuts(
            final Collection<Extension> extensions, boolean custom, InstrumentationType type, boolean isAttsEnabled) {
        List<ExtensionClassAndMethodMatcher> pointCutsOut = new ArrayList<>();

        if (extensions != null) {
            // the key is the class name
            Map<String, MethodMapper> classesToMethods = new HashMap<>();
            for (Extension ext : extensions) {
                if (ext.isEnabled()) {
                    pointCutsOut.addAll(convertToEnabledPointCuts(ext, ext.getName(), classesToMethods, custom, type,
                            isAttsEnabled));
                } else {
                    Agent.LOG.log(Level.WARNING, MessageFormat.format(
                            "Extension {0} is not enabled and so will not be instrumented.", ext.getName()));
                }
            }
        }

        return pointCutsOut;
    }

    /**
     * Takes in an xml extension object and converts it to a list of point cuts.
     *
     * @return The list of point cuts.
     */
    private static List<ExtensionClassAndMethodMatcher> convertToEnabledPointCuts(final Extension extension,
            final String extensionName, Map<String, MethodMapper> classesToMethods, boolean custom,
            InstrumentationType type, boolean isAttsEnabled) {
        List<ExtensionClassAndMethodMatcher> pointCutsOut = new ArrayList<>();

        if (extension != null) {
            String defaultMetricPrefix = createDefaultMetricPrefix(extension.getInstrumentation(), custom);

            List<Pointcut> inCuts = extension.getInstrumentation().getPointcut();
            if (inCuts != null && !inCuts.isEmpty()) {
                for (Pointcut cut : inCuts) {
                    try {

                        ExtensionClassAndMethodMatcher pc = createPointCut(extension, cut, defaultMetricPrefix,
                                extensionName, classesToMethods, custom, type, isAttsEnabled);
                        if (pc != null) {
                            logPointCutCreation(pc);
                            pointCutsOut.add(pc);
                        }
                    } catch (Exception e) {
                        String msg = MessageFormat.format(XmlParsingMessages.GEN_PC_ERROR, extensionName, e.toString());
                        Agent.LOG.log(Level.SEVERE, msg);
                        Agent.LOG.log(Level.FINER, msg, e);
                    }
                }
            } else {
                String msg = MessageFormat.format("There were no point cuts in the extension {0}.", extensionName);
                Agent.LOG.log(Level.INFO, msg);
            }
        }

        return pointCutsOut;
    }

    /**
     * Creates the default metric prefix string.
     * 
     * @return The metric prefix to be used if a metricName is not specified.
     */
    private static String createDefaultMetricPrefix(final Instrumentation instrument, boolean custom) {
        String metricPrefix = custom ? MetricNames.CUSTOM : MetricNames.JAVA;
        if (instrument != null) {
            String prefix = instrument.getMetricPrefix();
            if ((prefix != null) && prefix.length() != 0) {
                metricPrefix = prefix;
            }
        }
        return metricPrefix;
    }

    /**
     * Logs the creation of the point cut.
     * 
     * @param pc The point cut that was created.
     */
    private static void logPointCutCreation(final ExtensionClassAndMethodMatcher pc) {
        String msg = MessageFormat.format("Extension instrumentation point: {0} {1}", pc.getClassMatcher(),
                pc.getMethodMatcher());
        Agent.LOG.finest(msg);
    }

    /**
     * Converts the xml point cut into a java agent point cut.
     * 
     * @return The point cut object with the matchers set.
     */
    private static ExtensionClassAndMethodMatcher createPointCut(Extension extension, final Pointcut cut,
            final String metricPrefix, final String pName, final Map<String, MethodMapper> classesToMethods,
            boolean custom, InstrumentationType type, boolean isAttsEnabled) throws XmlException {
        ClassMatcher classMatcher;
        if (cut.getMethodAnnotation() != null) {
            classMatcher = new AllClassesMatcher();
        } else {
            classMatcher = createClassMatcher(cut, pName);
        }
        MethodMatcher methodMatcher = createMethodMatcher(cut, pName, classesToMethods);
        List<ParameterAttributeName> reportedParams = null;
        if (!isAttsEnabled) {
            // looks like this could be modified later
            reportedParams = new ArrayList<>();
        } else {
            reportedParams = getParameterAttributeNames(cut.getMethod());
        }

        return new ExtensionClassAndMethodMatcher(extension, cut, metricPrefix, classMatcher, methodMatcher, custom,
                reportedParams, type);
    }

    private static List<ParameterAttributeName> getParameterAttributeNames(List<Method> methods) {

        List<ParameterAttributeName> reportedParams = new ArrayList<>();
        for (Method m : methods) {
            if (m.getParameters() != null && m.getParameters().getType() != null) {
                for (int i = 0; i < m.getParameters().getType().size(); i++) {
                    com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters.Type t = m.getParameters().getType().get(
                            i);
                    if (t.getAttributeName() != null) {
                        try {
                            MethodMatcher methodMatcher = MethodMatcherUtility.createMethodMatcher("DummyClassName", m,
                                    new HashMap<String, MethodMapper>(), "");
                            ParameterAttributeName reportedParam = new ParameterAttributeName(i, t.getAttributeName(),
                                    methodMatcher);
                            reportedParams.add(reportedParam);
                        } catch (Exception e) {
                            Agent.LOG.log(Level.FINEST, e, e.getMessage());
                        }

                    }
                }
            }
        }
        return reportedParams;
    }

    /**
     * Creates the method matcher to be used in the point cut.
     * 
     * @param cut The xml information about the methods contain in the point cut.
     * @param pExtName The name of this extension.
     * @param classesToMethods The class/method combos which have already appeared.
     * @return The matcher to be used for methods on the point cut.
     * @throws XmlException
     */
    private static MethodMatcher createMethodMatcher(final Pointcut cut, final String pExtName,
            final Map<String, MethodMapper> classesToMethods) throws XmlException {
        List<Method> methods = cut.getMethod();
        List<String> traceReturnTypeDescriptors = cut.getTraceReturnTypeDescriptors();
        if (methods != null && !methods.isEmpty()) {
            return MethodMatcherUtility.createMethodMatcher(getClassName(cut), methods, classesToMethods, pExtName);
        } else if (cut.getMethodAnnotation() != null) {
            return new AnnotationMethodMatcher(Type.getObjectType(cut.getMethodAnnotation().replace('.', '/')));
        } else if (cut.isTraceLambda()) {
            return new LambdaMethodMatcher(cut.getPattern(), cut.getIncludeNonstatic());
        } else if (traceReturnTypeDescriptors != null && !traceReturnTypeDescriptors.isEmpty()) {
            return new ReturnTypeMethodMatcher(traceReturnTypeDescriptors);
        } else {
            throw new XmlException(MessageFormat.format(XmlParsingMessages.NO_METHOD, pExtName));
        }

    }

    static boolean isReturnTypeOkay(Type returnType) {
        if (returnType.getSort() == Type.ARRAY) {
            return isReturnTypeOkay(returnType.getElementType());
        }
        return returnType.getSort() == Type.OBJECT;
    }

    public static String getClassName(Pointcut cut) {
        if (cut.getClassName() != null) {
            return cut.getClassName().getValue().trim();
        } else if (cut.getInterfaceName() != null) {
            return cut.getInterfaceName().trim();
        } else {
            return null;
        }
    }

    /**
     * Create the class matcher to be used for the point cut.
     * 
     * @param pointcut The pointcut.
     * @param pExtName The name of the extension.
     * @return The class matcher created from the point cut.
     * @throws XmlException
     */
    static ClassMatcher createClassMatcher(final Pointcut pointcut, final String pExtName) throws XmlException {
        ClassName className = pointcut.getClassName();
        if (className != null) {
            if (className.getValue() == null || className.getValue().isEmpty()) {
                throw new XmlException("");
            }
            if (className.isIncludeSubclasses()) {
                return new ChildClassMatcher(className.getValue(), false);
            } else {
                return new ExactClassMatcher(className.getValue());
            }
        } else if (pointcut.getInterfaceName() != null) {
            return new InterfaceMatcher(pointcut.getInterfaceName());
        } else {
            throw new XmlException(MessageFormat.format(XmlParsingMessages.NO_CLASS_NAME, pExtName));
        }
    }
}
