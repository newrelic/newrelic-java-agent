package com.newrelic.agent.xml;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.objectweb.asm.Type;
import org.xml.sax.SAXException;

import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public class XmlInstrumentValidator {

    /**
     * The main instrumentation method. Validates the parameters, reads in the xml, and validates it.
     * 
     * @param cmd The command line parameters.
     */
    public static void validateInstrumentation(CommandLine cmd) {

        XmlInstrumentParams params = new XmlInstrumentParams();
        if (cmd == null) {
            printMessage("There were no command line parameters.");
        } else {

            try {
                if (verifyAndSetParameters(cmd, params)) {
                    validateInstrumentation(params);
                    printMessage(MessageFormat.format("PASS: The extension at {0} was successfully validated.",
                            params.getFile().getAbsoluteFile()));
                }

            } catch (IOException | SAXException e) {
                printMessage(MessageFormat.format("FAIL: The extension at {0} failed validation."
                        + " \nReason: {1} \nNOTE: set debug to true for more information.",
                        params.getFile().getAbsoluteFile(), e.getMessage()));
            } catch (IllegalArgumentException e) {
                // file can be null in here
                File file = params.getFile();
                String fileName = null;
                if (file != null) {
                    fileName = file.getAbsolutePath();
                }
                printMessage(MessageFormat.format("FAIL: The extension at {0} failed validation."
                        + " \n Reason: {1} \n Note: Set debug to true for more information.", fileName, e.getMessage()));
            } catch (ClassNotFoundException e) {
                printMessage(MessageFormat.format("FAIL: The extension at {0} failed validation."
                        + " \n Reason: The following class was not found: {1} \n Note: Set "
                        + "debug to true for more information.", params.getFile().getAbsoluteFile(), e.getMessage()));
            } catch (Exception e) {
                printMessage(MessageFormat.format("FAIL: The extension at {0} failed validation."
                        + " \n Reason: {1} \n Note: Set debug to true for more information.",
                        params.getFile().getAbsoluteFile(), e.getMessage()));
            }
        }
    }

    /**
     * Validates the instrumentation.
     * 
     * @param params The command line parameters.
     * @throws ClassNotFoundException Thrown if the class is not found.
     * @throws RuntimeException Thrown if a problem when converting the xml.
     * @throws IllegalArgumentException Thrown when the method or class can not be found on the class path.
     * @throws IOException Thrown if the file can not be read.
     * @throws SAXException Thrown if a problem parsing the document.
     */
    protected static void validateInstrumentation(final XmlInstrumentParams params) throws Exception {

        // read in the file - DOM Exception potentially thrown
        Extension extension = ExtensionDomParser.readFile(params.getFile());

        if (params.isDebug()) {
            System.out.println("Xml was successfully read. Starting processing.");
        }

        // attempt to convert to point cuts - RuntimeException
        // potentially thrown
        List<ExtensionClassAndMethodMatcher> convertedPcs = ExtensionConversionUtility.convertToPointCutsForValidation(extension);

        Instrumentation inst = extension.getInstrumentation();
        // this really has already been checked
        if (inst == null) {
            throw new RuntimeException("The instrumentation propery must be set for the extension.");
        }

        List<Pointcut> origPcs = inst.getPointcut();

        if (convertedPcs.size() != origPcs.size()) {
            throw new IllegalArgumentException("The processed number of point cuts does not match the"
                    + "original number of point cuts in the xml. Remove duplicates.");
        }

        for (int i = 0; i < convertedPcs.size(); i++) {
            MethodHolder holder = sortData(origPcs.get(i), params.isDebug());
            verifyPointCut(convertedPcs.get(i), holder);
            verifyAllMethodsAccounted(holder);
        }
    }

    /**
     * Verifies and sets the parameters.
     * 
     * @param cmd The command line parameters.
     * @param params Where the command line parameters will be stored once validated.
     * @return True if the input parameters are okay, else false.
     * @throws IllegalArgumentException
     */
    private static boolean verifyAndSetParameters(CommandLine cmd, XmlInstrumentParams params)
            throws IllegalArgumentException {
        try {
            XmlInstrumentOptions[] options = XmlInstrumentOptions.values();
            for (XmlInstrumentOptions ops : options) {
                ops.validateAndAddParameter(params, cmd.getOptionValues(ops.getFlagName()), ops.getFlagName());

            }
            return true;
        } catch (Exception e) {
            printMessage(MessageFormat.format("FAIL: The command line parameters are invalid. \n Reason: {0}",
                    e.getMessage()));
            return false;
        }
    }

    /**
     * Verifies that all methods have be removed from the method holder.
     * 
     * @param originals The original methods.
     */
    private static void verifyAllMethodsAccounted(MethodHolder originals) {
        if (originals.hasMethods()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "These methods are either duplicates, constructors, or are not present in the class: {0}",
                    originals.getCurrentMethods()));
        }
    }

    /**
     * Verifies the point cut.
     * 
     * @param cut The current point cut.
     * @param origMethods The original methods.
     * @throws ClassNotFoundException Thrown if the class can not be found.
     */
    private static void verifyPointCut(ExtensionClassAndMethodMatcher cut, MethodHolder origMethods)
            throws ClassNotFoundException {
        if (cut != null) {
            Collection<String> classNames = cut.getClassMatcher().getClassNames();

            for (String name : classNames) {
                // make sure the class is a valid class
                String nameDoted = name.replace("/", ".").trim();
                Class<?> theClass = Thread.currentThread().getContextClassLoader().loadClass(nameDoted);
                validateNoInterface(theClass);

                checkMethods(cut.getMethodMatcher(), theClass.getDeclaredMethods(), origMethods);

            }
        }
    }

    /**
     * Validates that the class is not an interface. Throws an illegal argument exception if the class is an interface.
     * 
     * @param theClass The current class to check.
     */
    private static void validateNoInterface(Class theClass) {
        if (theClass.isInterface()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Only classes can be implemented. This class is an interface: {0}", theClass.getName()));
        }
    }

    /**
     * Checks the methods in the class with the input methods.
     * 
     * @param matcher The method matcher associated with the class.
     * @param classMethods The methods on the class.
     * @param origMethods The original methods passed in.
     */
    private static void checkMethods(final MethodMatcher matcher, final java.lang.reflect.Method[] classMethods,
            final MethodHolder origMethods) {

        if (classMethods != null) {
            if (origMethods == null) {
                throw new IllegalArgumentException("Instrumenting a class not found in the XML.");
            }
            for (java.lang.reflect.Method m : classMethods) {
                String currentDesc = Type.getMethodDescriptor(m);

                // the method will be removed from the map if present
                checkPresenceAndMatcher(m.getName(), currentDesc, matcher, origMethods);
            }
        }
    }

    /**
     * Checks to see if the method or constructor was passed in and then verifies that it matches the matcher.
     * 
     * @param currentName Name of the method.
     * @param currentDesc Descriptor of the method.
     * @param matcher The matcher for the point cut,
     * @param origMethods The original methods passed in.
     */
    private static void checkPresenceAndMatcher(final String currentName, final String currentDesc,
            final MethodMatcher matcher, final MethodHolder origMethods) {
        if (origMethods.isMethodPresent(currentName, currentDesc, true)) {

            if (!matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, currentName, currentDesc,
                    MethodMatcher.UNSPECIFIED_ANNOTATIONS)) {
                throw new IllegalArgumentException(MessageFormat.format("The method was in the point cut"
                        + " but did not match the method matcher. Name: {0} Desc: {1}", currentName, currentDesc));
            }
        }
    }

    /**
     * Sorts the input data into a method holder for the specific point cut.
     * 
     * @param pc The original point cut in the XML.
     * @param debug True if debug should be turned on.
     * @return The data arranged into a map.
     */
    private static MethodHolder sortData(final Pointcut pc, final boolean debug) {
        MethodHolder cMethods = new MethodHolder(debug);
        if (pc != null) {
            cMethods.addMethods(pc.getMethod());

        }

        return cMethods;
    }

    /**
     * Prints the input message.
     * 
     * @param msg The message to be printed to standard out.
     */
    protected static void printMessage(final String msg) {
        System.out.println(msg);
    }

}
