package com.nr.instrumentation.builder;

import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

/**
 * Constructs a {@link WeavePackage} from the provided arguments by either scanning
 * classfiles in a set of directories or reading in a jar.
 */
public class WeavePackageFactory {
    WeavePackage createWeavePackage(String[] arguments) throws Exception {
        WeavePackageConfig config = WeavePackageConfig.builder().name("temp").build();

        if (representsDirectories(arguments)) {
            return createWeavePackageFromDirectories(config, arguments);
        } else if (representsSingleFile(arguments)) {
            try (FileInputStream fileInputStream = new FileInputStream(arguments[0]);
                 JarInputStream jarInStream = new JarInputStream(fileInputStream)) {
                return WeavePackage.createWeavePackage(jarInStream, config);
            }
        }

        throw new Exception("Arguments should be one file or one-or-more directories; found neither: " + Arrays.toString(arguments));
    }

    private WeavePackage createWeavePackageFromDirectories(WeavePackageConfig config, String[] directories) throws IOException {
        final ClassBytesVisitor classBytesVisitor = new ClassBytesVisitor();
        for (String directory : directories) {
            Files.walkFileTree(new File(directory).toPath(), classBytesVisitor);
        }

        return new WeavePackage(config, classBytesVisitor.getListOfClassBytes());
    }

    private boolean representsSingleFile(String[] arguments) {
        return arguments.length == 1 && new File(arguments[0]).isFile();
    }

    private boolean representsDirectories(String[] arguments) {
        return Stream.of(arguments).map(File::new).allMatch(file -> !file.exists() || file.isDirectory());
    }
}
