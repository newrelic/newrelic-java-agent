/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassClassFinder;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.JarUtils;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveClassInfo;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;
import com.newrelic.weave.weavepackage.NewClassAppender;
import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Utility class with for unit testing the weaver.
 */
public class WeaveTestUtils {

    /**
     * Keeps track of stuff we've added to the system classloader so we don't add it twice.
     * 
     * @see #addToContextClassloader(String, byte[])
     */
    private static final Table<String, String, PreparedMatch> MATCHES_ADDED_TO_CLASSLOADER = HashBasedTable.create();
    public static final String ERROR_TRAP = "com.newrelic.weave.WeaveTestUtils$ThrowingErrorTrapHandler";

    /**
     * Read a class into an ASM ClassNode represented with its full Java classname, e.g. "java.lang.Object"
     *
     * @param name full Java classname, e.g. "java.lang.Object"
     * @return ASM ClassNode representation of the class
     * @throws IOException
     */
    public static ClassNode readClass(String name) throws IOException {
        ClassReader reader = new ClassReader(name);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        reader.accept(result, ClassReader.SKIP_FRAMES);
        return result;
    }

    /**
     * Read a class into an ASM ClassNode represented with its full Java classname, e.g. "java.lang.Object"
     *
     * @param clazz class to read
     * @return ASM ClassNode representation of the class
     * @throws IOException
     */
    public static ClassNode readClass(Class<?> clazz) throws IOException {
        return readClass(clazz.getName());
    }

    /**
     * Match the original class to the weave class.
     *
     * @param original original class
     * @param weave weave class
     * @param isBaseMatch whether the match should be considered a base match
     * @return result of matching the original to the weave class
     * @throws IOException
     */
    public static ClassMatch match(Class<?> original, Class<?> weave, boolean isBaseMatch) throws IOException {
        return match(original, weave, isBaseMatch, Collections.<String>emptySet(), Collections.<String>emptySet());
    }

    /**
     * Match the original class to the weave class.
     *
     * @param original original class
     * @param weave weave class
     * @param isBaseMatch whether the match should be considered a base match
     * @return result of matching the original to the weave class
     * @throws IOException
     */
    public static ClassMatch match(Class<?> original, Class<?> weave, boolean isBaseMatch,
            Set<String> requiredAnnotations, Set<String> requiredMethodAnnotations) throws IOException {
        ClassNode originalNode = readClass(original);
        ClassNode weaveNode = readClass(weave);
        return ClassMatch.match(originalNode, weaveNode, isBaseMatch, requiredAnnotations, requiredMethodAnnotations, createContextCache());
    }

    /**
     * Create a class cache from the thread's context classloader suitable for testing.
     * 
     * @return class cache from the thread's context classloader
     */
    public static ClassCache createContextCache() {
        return new ClassCache(new ClassLoaderFinder(Thread.currentThread().getContextClassLoader()));
    }

    public static ClassMatch expectViolations(Class<?> original, Class<?> weave, boolean isBaseMatch,
            Set<String> requiredAnnotations, Set<String> requiredMethodAnnotations, WeaveViolation... expected)
            throws IOException {
        ClassMatch match = match(original, weave, isBaseMatch, requiredAnnotations, requiredMethodAnnotations);
        Collection<WeaveViolation> actual = match.getViolations();
        expectViolations(actual, expected);
        return match;
    }

    /**
     * Match the original to the weave class and assert that exactly the expected violations occur.
     *
     * @param original original class
     * @param weave weave class
     * @param isBaseMatch whether to use a base match
     * @param expected expected violations
     * @throws IOException
     */
    public static ClassMatch expectViolations(Class<?> original, Class<?> weave, boolean isBaseMatch,
            WeaveViolation... expected) throws IOException {
        return expectViolations(original, weave, isBaseMatch, Collections.<String> emptySet(), Collections.<String>emptySet(), expected);
    }

    /**
     * Assert that exactly the expected violations occur.
     *
     * @param result package validation result, containing actual violations
     * @param expected expected violations
     */
    public static void expectViolations(PackageValidationResult result, WeaveViolation... expected) {
        expectViolations(result.getViolations(), expected);
    }

    /**
     * Assert that exactly the expected violations occur.
     *
     * @param actual actual violations
     * @param expected expected violations
     */
    public static void expectViolations(Collection<WeaveViolation> actual, WeaveViolation... expected) {
        if (expected != null) {
            for (WeaveViolation expectedViolation : expected) {
                assertTrue("Expected violation: " + expectedViolation.toString(), actual.remove(expectedViolation));
            }
        }

        for (WeaveViolation unexpectedViolation : actual) {
            fail("Unexpected violation: " + unexpectedViolation.toString());
        }
    }

    /**
     * Validate the WeavePackage against a ClassLoader and assert that exactly the expected violations occur.
     * 
     * @param weavePackage package to validate
     * @param classloader classloader to validate against
     * @param expected exepcted violations
     * @throws IOException
     */
    public static void expectViolations(WeavePackage weavePackage, ClassLoader classloader, WeaveViolation... expected)
            throws IOException {
        List<WeaveViolation> actual = weavePackage.validate(new ClassCache(new ClassLoaderFinder(
                classloader))).getViolations();
        expectViolations(actual, expected);
    }

    public static void loadUtilityClasses(ClassLoader classloader, Map<String, byte[]> utilClasses) throws IOException {
        NewClassAppender.appendClasses(classloader, utilClasses);
    }

    public static Class<?> addToContextClassloader(ClassWeave classWeave, ClassCache classCache) throws IOException {
        Class<?> loadedClass = addToContextClassloader(classWeave.getComposite());
        PreparedMatch preparedMatch = classWeave.getMatch();
        // add extension if necessary
        if (preparedMatch.getExtension() != null) {
            addToContextClassloader(preparedMatch.getExtension().generateExtensionClass());
        }

        if (!preparedMatch.getAnnotationProxyClasses().isEmpty()) {
            for (Map.Entry<String, ClassNode> proxyClass : preparedMatch.getAnnotationProxyClasses().entrySet()) {
                addToContextClassloader(proxyClass.getKey().replaceAll("/", "\\."), WeaveUtils.convertToClassBytes(proxyClass.getValue(), classCache));
            }
        }

        // add new inner classes if necessary
        for (String newInnerClassName : preparedMatch.getNewInnerClasses()) {
            ClassNode newInnerClass = preparedMatch.prepareNewInnerClass(readClass(newInnerClassName));
            addToContextClassloader(newInnerClass);
        }
        return loadedClass;
    }

    /**
     * Add the class represented by the ASM node definition to the class loader
     *
     * @param node ASM class node definition
     * @return class instance that was loaded into the classloader
     */
    public static Class<?> addToContextClassloader(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        String name = node.name.replace('/', '.');
        return addToContextClassloader(name, bytes);
    }

    /**
     * From http://asm.ow2.org/doc/faq.html#Q5
     *
     * @param className class name
     * @param classBytes binary class data
     * @return class instance that was loaded into the classloader
     */
    public static synchronized Class<?> addToContextClassloader(String className, byte[] classBytes) {
        Class clazz = null;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class cls = Class.forName("java.lang.ClassLoader");
            java.lang.reflect.Method method = cls.getDeclaredMethod("defineClass", String.class, byte[].class,
                    int.class, int.class);

            method.setAccessible(true);
            try {
                Object[] args = new Object[] { className, classBytes, 0, classBytes.length };
                clazz = (Class) method.invoke(loader, args);
            } finally {
                method.setAccessible(false);
            }
        } catch (InvocationTargetException e) {
            String errorMessage = e.getCause().getMessage();
            if ((e.getCause() instanceof LinkageError) && errorMessage != null && errorMessage.contains("attempted") &&
                    errorMessage.contains("duplicate class definition")) {
                // The class is already defined. This is fine.
            } else {
                e.printStackTrace();
                throw new RuntimeException("Unable to load class " + className);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to load class " + className);
        }
        return clazz;
    }

    /**
     * Prints the node to standard out with the ClassReader.SKIP_DEBUG flag. Writes the class to a byte[] and reads it
     * back in using a ClassReader, so things like unnecessary lables are properly stripped. To see the raw node, use
     * {@link #printRaw}.
     *
     * @param node ASM class node representation to print to output
     * @throws IOException
     */
    public static void print(ClassNode node) throws IOException {
        print(node, System.out, ClassReader.SKIP_DEBUG);
    }

    /**
     * Print the raw class node.
     *
     * @param node ASM class node representation to print to output
     * @throws IOException
     */
    public static void printRaw(ClassNode node) throws IOException {
        node.accept(new TraceClassVisitor(new PrintWriter(System.out)));
    }

    /**
     * Print bytecode for the class node to the specified output using the specified ClassReader flags.
     *
     * @param node ASM class node representation to print to output
     * @param out stream to print to
     * @param flags ClassReader flags to filter output, e.g. ClassReader.SKIP_DEBUG
     * @throws IOException
     */
    public static void print(ClassNode node, PrintStream out, int flags) throws IOException {
        if (node == null) {
            return;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(new TraceClassVisitor(new PrintWriter(out)), flags);
    }

    /**
     * Generates a bytecode-like representation of the instruction. Useful when looking at instructions in a debugger.
     *
     * @param node instruction
     * @return bytecode-like representation of the instruction
     */
    public static String instructionToString(AbstractInsnNode node) {
        Textifier p = new Textifier(WeaveUtils.ASM_API_LEVEL) {
            @Override
            protected void appendLabel(Label l) {
                if (labelNames == null) {
                    labelNames = new HashMap<>();
                }
                String name = labelNames.get(l);
                if (name == null) {
                    name = l.toString();
                    labelNames.put(l, name);
                }
                stringBuilder.append(name);
            }
        };
        TraceMethodVisitor mv = new TraceMethodVisitor(p);
        node.accept(mv);
        StringWriter sw = new StringWriter();
        p.print(new PrintWriter(sw));
        return sw.toString().replace('\n', ' ');
    }

    public static ClassWeave weaveAnnotedTypeAndAddToContextClassloader(String originalName, String weaveClass,
            Set<String> requiredAnnotations, Set<String> requiredMethodAnnotations) throws IOException {
        ClassNode errorHandlerClassNode = WeaveTestUtils.readClass(
                ERROR_TRAP);
        return weaveAndAddToContextClassloader(originalName, weaveClass, originalName, false, requiredAnnotations,
                requiredMethodAnnotations, errorHandlerClassNode);
    }

    public static ClassWeave weaveAnnotedTypeAndAddToContextClassloader(String originalName, String weaveClass,
            Set<String> requiredAnnotations, Set<String> requiredMethodAnnotations, boolean isBaseMatch) throws IOException {
        ClassNode errorHandlerClassNode = WeaveTestUtils.readClass(
                ERROR_TRAP);
        return weaveAndAddToContextClassloader(originalName, weaveClass, originalName, isBaseMatch, requiredAnnotations,
                requiredAnnotations, errorHandlerClassNode);
    }

    public static ClassWeave weave(String originalName, String weaveName) throws IOException {
        ClassNode errorHandlerClassNode = WeaveTestUtils.readClass(
                ERROR_TRAP);
        return weave(originalName, weaveName, originalName, false, Collections.<String>emptySet(),
                Collections.<String>emptySet(), errorHandlerClassNode, ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE);
    }

    /**
     * Weaves the original class with the weave class, adds the result to the system classloader.
     *
     * @param originalName original Java classname, e.g. "java.lang.Object"
     * @param weaveName weave Java classname, e.g. "java.lang.Object"
     * @throws IOException
     */
    public static ClassWeave weaveAndAddToContextClassloader(String originalName, String weaveName) throws IOException {
        return weaveAndAddToContextClassloader(originalName, weaveName, originalName);
    }

    public static ClassWeave weaveAndAddToContextClassloader(ClassNode originalClass, ClassNode weaveClass)
            throws IOException {
        ClassNode errorHandlerClassNode = WeaveTestUtils.readClass(
                ERROR_TRAP);
        ClassCache contextCache = createContextCache();
        ClassWeave weave = weave(originalClass, weaveClass, originalClass, false,
                Collections.<String> emptySet(), Collections.<String>emptySet(), errorHandlerClassNode,
                ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE, contextCache);
        addToContextClassloader(weave, contextCache);
        return weave;
    }

    public static ClassWeave weaveAndAddToContextClassloader(String originalName, String weaveName, String targetName)
            throws IOException {
        ClassNode errorHandlerClassNode = WeaveTestUtils.readClass(
                ERROR_TRAP);
        boolean isBaseMatch = !originalName.equals(targetName);
        return weaveAndAddToContextClassloader(originalName, weaveName, targetName, isBaseMatch,
                Collections.<String> emptySet(), Collections.<String>emptySet(), errorHandlerClassNode);
    }

    public static ClassWeave weaveAndAddToContextClassloader(String originalName, String weaveName, String targetName,
            boolean isBaseMatch) throws IOException {
        ClassNode errorHandlerClassNode = WeaveTestUtils.readClass(
                ERROR_TRAP);
        return weaveAndAddToContextClassloader(originalName, weaveName, targetName, isBaseMatch,
                Collections.<String> emptySet(), Collections.<String>emptySet(), errorHandlerClassNode);
    }

    public static ClassNode getErrorHandler() throws IOException {
        return WeaveTestUtils.readClass(ERROR_TRAP);
    }

    public static ClassWeave weaveAndAddToContextClassloader(String originalName, String weaveName, String targetName,
            boolean isBaseMatch, Set<String> requiredClassAnnotations, Set<String> requiredMethodAnnotations, ClassNode errorHandlerClassNode)
            throws IOException {
        ClassWeave weave = weave(originalName, weaveName, targetName, isBaseMatch,
                requiredClassAnnotations, requiredMethodAnnotations, errorHandlerClassNode,
                ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE);
        addToContextClassloader(weave, createContextCache());
        return weave;
    }

    public static ClassWeave weave(ClassNode originalClass, ClassNode weaveClass,
            ClassNode target, boolean isBaseMatch, Set<String> requiredClassAnnotations,
            Set<String> requiredMethodAnnotations, ClassNode errorHandlerClassNode, ClassNode extensionTemplate,
            ClassCache contextCache) throws IOException {

        // match original and weave only if we haven't already done so
        PreparedMatch preparedMatch = MATCHES_ADDED_TO_CLASSLOADER.get(originalClass.name, weaveClass.name);
        if (preparedMatch == null) {
            WeaveClassInfo weaveClassInfo = new WeaveClassInfo(weaveClass);

            // preprocess weave class if original is an interface
            if ((originalClass.access & Opcodes.ACC_INTERFACE) != 0) {
                ClassNode preprocessed = new ClassNode(WeaveUtils.ASM_API_LEVEL);
                Map<String, MatchType> weaveMatches = new HashMap<>();
                weaveMatches.put(weaveClassInfo.getOriginalName(), MatchType.Interface);
                weaveMatches.put(weaveClass.name, MatchType.Interface);
                ClassVisitor preprocessor = MethodProcessors.fixInvocationInstructions(preprocessed, weaveMatches);
                weaveClass.accept(preprocessor);
                weaveClass = preprocessed;
            }

            ClassMatch match = ClassMatch.match(originalClass, weaveClass, isBaseMatch, requiredClassAnnotations,
                    requiredMethodAnnotations, contextCache);

            // weave should not proceed in case of match errors
            for (String newInnerClassName : match.getNewInnerClasses()) {
                match.validateNewInnerClass(readClass(newInnerClassName));
            }
            Collection<WeaveViolation> violations = match.getViolations();
            String assertMsg = String.format("Encountered %d match violations: %s", violations.size(),
                    Iterables.toString(violations));
            assertTrue(assertMsg, violations.isEmpty());

            preparedMatch = PreparedMatch.prepare(match, errorHandlerClassNode, extensionTemplate, true);
            MATCHES_ADDED_TO_CLASSLOADER.put(originalClass.name, weaveClass.name, preparedMatch);
        }

        // weave target using the specified match and add composite class to classloader
        return ClassWeave.weave(preparedMatch, target, null, Collections.emptyMap());
    }

    /**
     * Weaves the original class with the weave class, adds the result to the system classloader.
     *
     * @param originalName original Java classname, e.g. "java.lang.Object"
     * @param weaveName weave Java classname, e.g. "java.lang.Object"
     * @param targetName target Java classname, e.g. "java.lang.Object"
     * @param errorHandlerClassNode a class node of an {@link ErrorTrapHandler} class.
     * @throws IOException
     */
    public static ClassWeave weaveAndAddToContextClassloader(String originalName, String weaveName, String targetName,
            boolean isBaseMatch, Set<String> requiredClassAnnotations, Set<String> requiredMethodAnnotations,
            ClassNode errorHandlerClassNode, ClassNode extensionTemplate) throws IOException {
        ClassNode originalClass = readClass(originalName);
        ClassNode weaveClass = readClass(weaveName);
        ClassNode target = originalName.equals(targetName) ? originalClass : readClass(targetName);
        ClassCache contextCache = createContextCache();
        ClassWeave weave = weave(originalClass, weaveClass, target, isBaseMatch,
                requiredClassAnnotations, requiredMethodAnnotations, errorHandlerClassNode, extensionTemplate, contextCache);
        addToContextClassloader(weave, contextCache);
        return weave;

    }

    public static ClassWeave weave(String originalName, String weaveName, String targetName, boolean isBaseMatch,
            Set<String> requiredClassAnnotations, Set<String> requiredMethodAnnotations, ClassNode errorHandlerClassNode,
            ClassNode extensionTemplate) throws IOException {
        ClassNode originalClass = readClass(originalName);
        ClassNode weaveClass = readClass(weaveName);
        ClassNode target = originalName.equals(targetName) ? originalClass : readClass(targetName);
        return weave(originalClass, weaveClass, target, isBaseMatch, requiredClassAnnotations, requiredMethodAnnotations,
                errorHandlerClassNode, extensionTemplate, createContextCache());
    }

    /**
     * Get bytes representing the specified class name.
     *
     * @param classname class name
     * @return byte[] of the class
     * @throws IOException if class cannot be found/read
     */
    public static byte[] getClassBytes(String classname) throws IOException {
        return WeaveUtils.getClassBytesFromClassLoaderResource(classname,
                Thread.currentThread().getContextClassLoader());
    }

    /**
     * Convert an array of bytes into a file.
     *
     * @param bytes class bytes
     * @param loc location to write the file
     */
    public static void byteArrayToFile(byte[] bytes, String loc) throws IOException {
        // convert array of bytes into file
        FileOutputStream fileOuputStream = new FileOutputStream(loc);
        fileOuputStream.write(bytes);
        fileOuputStream.close();
    }

    /**
     * Check if a class is loaded by a classloader
     *
     * @param classloader the classloader to check
     * @param className internal or canonical name of a class
     */
    public static boolean isClassLoaded(ClassLoader classloader, String className) throws Exception {
        className = className.replace('/', '.');

        Class<?> cls = ClassLoader.class;
        java.lang.reflect.Method method = cls.getDeclaredMethod("findLoadedClass", String.class);
        method.setAccessible(true);
        try {
            Object[] args = new Object[] { className };
            Object clazz = method.invoke(classloader, args);
            return null != clazz;
        } finally {
            method.setAccessible(false);
        }
    }

    /**
     * Find the parent this (e.g. this$0) field - we assume it's the first synthetic field in the class.
     * 
     * @param innerClass inner class to search
     * @return parent this (e.g. this$0) field
     */
    public static String findParentThisFieldName(Class<?> innerClass) {
        String parentThisFieldName = null;
        for (Field field : innerClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                parentThisFieldName = field.getName();
                break;
            }
        }
        return parentThisFieldName;
    }

    public static class ThrowingErrorTrapHandler extends ErrorTrapHandler {

        /**
         * An error trap that rethrows the exception. Used only for testing as it is functionally equivalent to no error
         * trap.
         * 
         * @throws Throwable
         */
        public static void onWeaverThrow(Throwable weaverError) throws Throwable {
            throw weaverError;
        }

    }

    /**
     * Create a jar containing the given classes.
     */
    public static URL createJarFile(String prefix, Class<?>... classes) throws IOException {
        return createJarFile(prefix, null, classes);
    }

    /**
     * Create a jar containing the given classes.
     */
    public static URL createJarFile(String prefix, Manifest manifest, Class<?>... classes) throws IOException {
        ClassLoader loader = WeaveTestUtils.class.getClassLoader();
        Map<String, byte[]> bytes = new HashMap<>(classes.length);
        for (int i = 0; i < classes.length; ++i) {
            Class<?> clazz = classes[i];
            URL resourceLoc = new ClassClassFinder(clazz).findResource(clazz.getName());
            byte[] clazzBytes = Streams.read(resourceLoc.openStream(), true);
            bytes.put(clazz.getName(), clazzBytes);
        }
        return JarUtils.createJarFile(prefix, bytes, manifest).toURI().toURL();
    }
}
