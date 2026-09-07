/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.info;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import com.jdheim.toolfetch.service.util.OperatingSystemPredicates;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/// Resolves the Java home directory from the first valid Java executable found on the `PATH` environment variable.
public final class JavaHome {

    static final String JAVA_EXEC = "java";

    static final String JAVA_EXEC_WINDOWS = JAVA_EXEC + ".exe";

    static final Path MACOS_JAVA_STUB = macOsJavaStub();

    private static final String PATH_ENV_VAR = "PATH";

    private static final String JDK8_JRE_DIR = "jre";

    private JavaHome() {
        throw new AssertionError();
    }

    public static @Nullable Path get() {
        return LazyHolder.VALUE;
    }

    static @Nullable Path resolveJavaHomePath() {
        String pathEnv = pathEnv();
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }
        String javaExecutableName = OperatingSystemPredicates.isWindows() ? JAVA_EXEC_WINDOWS : JAVA_EXEC;
        for (String pathEntry : StringUtils.splitPreserveAllTokens(pathEnv, File.pathSeparatorChar)) {
            Path java = javaPath(pathEntry, javaExecutableName);
            Path realJava = realJavaPath(java);
            if (realJava == null) {
                continue;
            }
            Path bin = realJava.getParent();
            if (bin != null) {
                Path home = bin.getParent();
                if (home != null) {
                    return resolveJavaHomePath(home);
                }
            }
        }
        return null;
    }

    static String pathEnv() {
        return System.getenv(PATH_ENV_VAR);
    }

    private static @Nullable Path javaPath(String pathEntry, String javaExecutableName) {
        if (pathEntry.isEmpty()) {
            return null;
        }
        try {
            Path java = Path.of(pathEntry, javaExecutableName);
            return Files.isRegularFile(java) && Files.isExecutable(java) ? java : null;
        } catch (InvalidPathException _) {
            return null;
        }
    }

    private static @Nullable Path realJavaPath(@Nullable Path java) {
        if (java == null) {
            return null;
        }
        try {
            Path realJava = java.toRealPath();
            return OperatingSystemPredicates.isMacOs() && MACOS_JAVA_STUB.equals(realJava) ? null : realJava;
        } catch (IOException _) {
            return null;
        }
    }

    @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "Known MacOS system wrapper tool that locates and executes the actual java executables")
    private static Path macOsJavaStub() {
        return Path.of("/usr/bin/java");
    }

    private static Path resolveJavaHomePath(Path home) {
        Path homeName = home.getFileName();
        if (homeName != null && JDK8_JRE_DIR.equalsIgnoreCase(homeName.toString())) {
            Path jdk8Home = home.getParent();
            if (jdk8Home != null) {
                return jdk8Home;
            }
        }
        return home;
    }

    static final class LazyHolder {

        private static final @Nullable Path VALUE = resolveJavaHomePath();

        private LazyHolder() {
            throw new AssertionError();
        }

    }

}
