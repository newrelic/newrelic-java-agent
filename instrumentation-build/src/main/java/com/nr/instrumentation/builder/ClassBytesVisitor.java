package com.nr.instrumentation.builder;

import com.newrelic.weave.utils.Streams;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This {@link FileVisitor} implementation finds all files that end
 * with {@literal .class} and reads their bytes into a {@link List}.
 */
class ClassBytesVisitor implements FileVisitor<Path> {
    private final List<byte[]> files = new ArrayList<>();

    public List<byte[]> getListOfClassBytes() {
        return Collections.unmodifiableList(files);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toFile().getName().endsWith(".class")) {
            try (InputStream is = new FileInputStream(file.toFile())) {
                files.add(Streams.read(is, false));
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
    }
}
