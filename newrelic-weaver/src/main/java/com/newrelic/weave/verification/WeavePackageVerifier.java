/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.verification;

import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.weavepackage.CachedWeavePackage;
import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;

/**
 * Verifies that a weave package will load or not for a specific classpath.
 */
public class WeavePackageVerifier {

    private final PrintStream out;
    private final String instrumentationJar;
    private final List<String> userJars;

    /**
     * Run the verifier. The arguments are:
     * <li>
     * <ol>
     * The weave package JAR file
     * </ol>
     * <ol>
     * The expected result, either <code>true</code> or <code>false</code>
     * </ol>
     * <ol>
     * Classpath JAR file
     * </ol>
     * <ol>
     * Another classpath JAR file
     * </ol>
     * <ol>
     * ...
     * </ol>
     * </li>
     */
    public static void main(String[] args) {
        String instrumentationJar = args[0];
        boolean expectedVerificationResult = Boolean.valueOf(args[1]);
        String taskName = args[2];
        boolean printSuccess = Boolean.valueOf(args[3]);
        List<String> userJars = new ArrayList<>();
        if (args.length > 4) {
            userJars = Arrays.asList(args).subList(4, args.length);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream output = new PrintStream(outputStream);
            boolean passed = WeavePackageVerifier.verify(output, instrumentationJar, userJars);

            output.println(taskName);

            if (passed == expectedVerificationResult) {
                if (printSuccess) {
                    output.println("SUCCEEDED");
                    System.out.println(new String(outputStream.toByteArray()));
                }
            } else {
                output.println("FAILED");
                output.println("-- Expected verification result:" + expectedVerificationResult);
                output.println("-- Actual verification result:" + passed);
                System.err.println(new String(outputStream.toByteArray()));
                throw new RuntimeException("FAILED");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run validation using the weave package and classpath and return whether or not the weave package verified.
     * 
     * @param out {@link PrintStream} to write messages to
     * @param instrumentationJar weave package JAR file
     * @param userJars JAR files for the classpath
     * @return verification result
     */
    public static boolean verify(PrintStream out, String instrumentationJar, List<String> userJars) throws Exception {
        return new WeavePackageVerifier(out, instrumentationJar, userJars).verify();
    }

    private WeavePackageVerifier(PrintStream out, String instrumentationJar, List<String> userJars) {
        this.out = out;
        this.instrumentationJar = instrumentationJar;
        this.userJars = userJars;
    }

    /**
     * Run validation using the weave package and classpath and return whether or not the weave package verified.
     * 
     * @return whether or not the weave package verified
     */
    private boolean verify() throws Exception {
        WeavePackage weavePackage = getWeavePackage(instrumentationJar);

        ClassLoader loader = createClassloaderForVerification(userJars);
        ClassCache cache = new ClassCache(new ClassLoaderFinder(loader));
        PackageValidationResult result = weavePackage.validate(cache);
        if (!result.succeeded()) {
            for (WeaveViolation violation : result.getViolations()) {
                out.println(violation.toString());
            }
        }
        return result.succeeded();
    }

    /**
     * Create a {@link WeavePackage} for the specified instrumentation JAR
     * 
     * @param instrumentationJar instrumentation JAR
     * @return {@link WeavePackage} for the specified instrumentation JAR
     */
    private WeavePackage getWeavePackage(String instrumentationJar) throws Exception {
        URL instrumentationJarUrl = new File(instrumentationJar).toURI().toURL();

        JarInputStream jarStream = null;
        try {
            WeavePackageConfig config = WeavePackageConfig.builder().url(instrumentationJarUrl).build();
            jarStream = new JarInputStream(instrumentationJarUrl.openStream());
            return CachedWeavePackage.createWeavePackage(jarStream, config);
        } finally {
            if (null != jarStream) {
                jarStream.close();
            }
        }
    }

    /**
     * Create a classloader for loading "user" code.
     *
     * @param jars JARs on the classpath
     * @return {@link ClassLoader} suitable for verification
     * @throws MalformedURLException
     */
    private ClassLoader createClassloaderForVerification(List<String> jars) throws MalformedURLException {
        Set<URL> urls = new HashSet<>();
        out.println("Creating user classloader with custom classpath:");
        for (String s : jars) {
            File jarFile = new File(s);
            if (!jarFile.exists()) {
                out.println(String.format("\tWARNING: Given jar does not exist: %s", s));
            }
            urls.add(jarFile.toURI().toURL());
            out.println(String.format("\t%s", s));
        }
        return new VerificationClassLoader(urls.toArray(new URL[urls.size()]));
    }
}
