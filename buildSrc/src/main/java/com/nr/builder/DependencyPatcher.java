/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import com.nr.builder.patcher.ModifyReferencesToLog4j2Plugins;
import com.nr.builder.patcher.RedirectGetLoggerCalls;
import com.nr.builder.patcher.UnmappedDependencyErrorGenerator;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.logging.Logging;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import shadow.org.apache.tools.zip.ZipEntry;
import shadow.org.apache.tools.zip.ZipOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class combines three patch tasks into one. This works around a limitation
 * where individual transformers can't apply to more than one class in a given
 * shadowJar task. Although the files impacted by the below are currently disjoint,
 * it's better to be defensive.
 */
public class DependencyPatcher implements Transformer {

    private static final String AGENT_DEPENDENCY_PACKAGE_PREFIX = "com/newrelic/agent/deps/";

    private static final Logger logger = Logging.getLogger(DependencyPatcher.class);

    private static final List<Patcher> patchers = Arrays.asList(
            new ModifyReferencesToLog4j2Plugins(),
            new RedirectGetLoggerCalls(),
            new UnmappedDependencyErrorGenerator()
    );

    private final List<TransformedFile> transformedFiles = new LinkedList<>();

    /**
     * This method should return true for any file we might transform. Note that
     * even if we <i>don't</i> decide to transform it after all, we <i>still</i>
     * have to save it off and put it in the output stream.
     *
     * @return true if we will at least look at the class
     */
    @Override
    public boolean canTransformResource(FileTreeElement element) {
        if (!element.getName().endsWith(".class") || !element.getPath().startsWith(AGENT_DEPENDENCY_PACKAGE_PREFIX)) {
            return false;
        }

        AtomicBoolean shouldRetransform = new AtomicBoolean(false);
        try (InputStream classStream = element.open()) {
            ClassReader reader = new ClassReader(classStream);
            ClassVisitor patchVisitor = null;
            for (Patcher patcher : patchers) {
                patchVisitor = patcher.getVerificationVisitor(patchVisitor, shouldRetransform);
            }
            reader.accept(patchVisitor, ClassReader.EXPAND_FRAMES);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read classfile " + element.getPath(), e);
        } catch (PatcherViolationException e) {
            throw new RuntimeException("Error found in classfile " + element.getPath(), e);
        }
        if (shouldRetransform.get()) {
            logger.info("Planning to transform class " + element.getPath());
        }
        return shouldRetransform.get();
    }

    /**
     * Stashes a copy of the file, transforming it if appropriate.
     *
     * @param context Handles to the file that we are going to transform.
     */
    @Override
    public void transform(TransformerContext context) {
        if (!context.getPath().endsWith(".class") || !context.getPath().startsWith(AGENT_DEPENDENCY_PACKAGE_PREFIX)) {
            throw new RuntimeException("Should not transform non-class, non-dependency file " + context.getPath());
        }

        TransformedFile transformedFile = new TransformedFile();
        transformedFile.destinationPath = context.getPath();

        try {
            transformedFile.tempFile = File.createTempFile("class", ".class");
        } catch (IOException e) {
            throw new RuntimeException("Unable to create tempfile.", e);
        }

        logger.info("About to transform {} to file {}", transformedFile.destinationPath, transformedFile.tempFile.getAbsolutePath());

        ClassWriter writer = new ClassWriter(0);

        ClassVisitor patchVisitor = writer;
        for (Patcher patcher : patchers) {
            patchVisitor = patcher.getRewritingVisitor(patchVisitor);
        }

        ClassReader reader;
        try {
            reader = new ClassReader(context.getIs());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the class stream.", e);
        }

        reader.accept(patchVisitor, ClassReader.EXPAND_FRAMES);
        byte[] classBytes = writer.toByteArray();
        logger.info("Transformed {} successfully", transformedFile.destinationPath);

        try (
                InputStream classByteInput = new ByteArrayInputStream(classBytes);
                OutputStream classByteOutput = new FileOutputStream(transformedFile.tempFile)
        ) {
            transformedFile.size = Streams.copy(classByteInput, classByteOutput);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to temporary file " + transformedFile.tempFile.getAbsolutePath(), e);
        }
        logger.info("Wrote {} successfully", transformedFile.destinationPath);

        transformedFiles.add(transformedFile);
    }

    /**
     * true if we have received any files to our {@link #transform} method,
     * regardless of whether or not we have actually transformed any.
     *
     * @return true if we have received any files to our {@link #transform} method
     */
    @Override
    public boolean hasTransformedResource() {
        return transformedFiles.size() > 0;
    }

    /**
     * Add the transformed zip entries to the output stream.
     *
     * @param jos The JarOutputStream to which we're writing files
     * @param preserveFileTimestamps True if the file timestamps should be preserved from the original
     */
    @Override
    public void modifyOutputStream(ZipOutputStream jos, boolean preserveFileTimestamps) {
        for (TransformedFile transformedFile : transformedFiles) {
            ZipEntry entry = new ZipEntry(transformedFile.destinationPath);
            entry.setSize(transformedFile.size);
            try {
                jos.putNextEntry(entry);
            } catch (IOException e) {
                throw new RuntimeException("Unable to add entry to the jar", e);
            }

            try (InputStream fromFile = new FileInputStream(transformedFile.tempFile)) {
                Streams.copy(fromFile, jos);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to write " + transformedFile.destinationPath + " from " + transformedFile.tempFile.getAbsolutePath() + " to the jar", e);
            }

            logger.info("Added {} to the output jar", transformedFile.destinationPath);
        }
    }

    private static class TransformedFile {
        File tempFile;
        String destinationPath;
        long size;
    }
}
