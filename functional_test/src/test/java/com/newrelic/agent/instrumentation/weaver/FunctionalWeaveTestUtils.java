/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.instrumentation.context.ClassChecker;
import com.newrelic.agent.instrumentation.context.FinalClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.utils.Streams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;

public class FunctionalWeaveTestUtils {

    public static byte[] renameClass(String weaveClassName, final String newName) throws IOException {
        InputStream resourceAsStream = FunctionalWeaveTestUtils.class.getClassLoader().getResourceAsStream(
                Utils.getClassResourceName(weaveClassName));
        if (resourceAsStream == null) {
            resourceAsStream = FunctionalWeaveTestUtils.class.getClassLoader().getResourceAsStream(
                    Utils.getClassResourceName(newName));
            return Streams.read(resourceAsStream, true);
        }
        ClassReader reader = new ClassReader(resourceAsStream);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        Remapper remapper = new SimpleRemapper(weaveClassName, newName);
        ClassRemapper cv = new ClassRemapper(writer, remapper);
        reader.accept(cv, ClassReader.SKIP_FRAMES);

        return writer.toByteArray();
    }

    public static void loadWeaveTestInstrumentation() throws Exception {
        InstrumentationContextManager contextManager = ServiceFactory.getClassTransformerService().getContextManager();

        Collection<File> weaveExtensions = new HashSet<>();
        File file = new File("weave_test/build/libs/weave_test-1.0.jar");
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        weaveExtensions.add(file);
        Agent.LOG.log(Level.FINER, "Adding Weave Test package");
        contextManager.getClassWeaverService().reloadExternalWeavePackages(weaveExtensions, new HashSet<File>());
    }

    public static void reinstrument(String... originalInternalNames) throws Exception {
        loadWeaveTestInstrumentation();
        for (String name : originalInternalNames) {
            try {
                InstrumentTestUtils.retransformClass(Type.getObjectType(name).getClassName());
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void addClassChecker() {
        FinalClassTransformer.setClassChecker(new ClassChecker() {
            @Override
            public void check(byte[] bytecode) {
                CheckClassAdapter.verify(new ClassReader(bytecode), false, new PrintWriter(System.err));
            }
        });
    }

}
