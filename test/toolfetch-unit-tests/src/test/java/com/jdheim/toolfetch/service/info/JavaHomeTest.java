/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import com.jdheim.toolfetch.service.util.OperatingSystemPredicates;
import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import com.jdheim.toolfetch.step.file.FileSteps;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/// OOC Tests for [JavaHome]
class JavaHomeTest {

    @TempDir
    Path tempDir;

    @Test
    void testNotInstantiable() {
        AssertionSteps.assertNotInstantiable(JavaHome.class);
    }

    @Test
    void testLazyHolderNotInstantiable() {
        AssertionSteps.assertNotInstantiable(JavaHome.LazyHolder.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "   ")
    void testResolveJavaHomePath_MissingOrBlankPath(@Nullable String pathEnv) {
        assertThat(resolveJavaHomePath(pathEnv)).isNull();
    }

    @Test
    void testResolveJavaHomePath_InvalidPath() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("directory"));
        String pathEnv = String.join(File.pathSeparator, "", "\0", directory.toString());

        assertThat(resolveJavaHomePath(pathEnv)).isNull();
    }

    @Test
    void testResolveJavaHomePath() {
        Path javaHome = Objects.requireNonNull(JavaHome.get());
        assertThat(javaHome).isNotNull().isDirectory().isNotEmptyDirectory();
        Path javaExecutable = javaHome.resolve("bin", javaExecutableName());
        assertThat(javaExecutable).isNotNull().isRegularFile().isExecutable();
    }

    @Test
    void testResolveJavaHomePath_Jdk8JrePath() throws IOException {
        Path javaHome = tempDir.resolve("jdk");
        Path javaBin = Files.createDirectories(javaHome.resolve("jre/bin"));
        Path java = Files.createFile(javaBin.resolve(javaExecutableName()));
        FileSteps.setExecutable(java);

        String pathEnv = String.join(File.pathSeparator, tempDir.resolve("missing").toString(), javaBin.toString());
        assertThat(resolveJavaHomePath(pathEnv)).isEqualTo(javaHome);
    }

    @Test
    void testResolveJavaHomePath_NonExecJava() throws IOException {
        Path javaHome = tempDir.resolve("jdk");
        Path javaBin = Files.createDirectories(javaHome.resolve("bin"));
        Files.createFile(javaBin.resolve(javaExecutableName()));

        String pathEnv = String.join(File.pathSeparator, tempDir.resolve("missing").toString(), javaBin.toString());
        assertThat(resolveJavaHomePath(pathEnv)).isNull();
    }

    @Test
    void testResolveJavaHomePath_WindowsJavaExec() throws IOException {
        Path javaHome = tempDir.resolve("jdk");
        Path javaBin = Files.createDirectories(javaHome.resolve("bin"));
        Path java = Files.createFile(javaBin.resolve(JavaHome.JAVA_EXEC_WINDOWS));
        FileSteps.setExecutable(java);

        try (MockedStatic<OperatingSystemPredicates> operatingSystemPredicates = mockStatic(OperatingSystemPredicates.class,
                Mockito.CALLS_REAL_METHODS)) {
            operatingSystemPredicates.when(OperatingSystemPredicates::isWindows).thenReturn(true);

            assertThat(resolveJavaHomePath(javaBin.toString())).isEqualTo(javaHome);
        }
    }

    @Test
    void testResolveJavaHomePath_MacOsJavaExec() throws IOException {
        Path javaHome = tempDir.resolve("jdk");
        Path javaBin = Files.createDirectories(javaHome.resolve("bin"));
        Path java = Files.createFile(javaBin.resolve(javaExecutableName()));
        FileSteps.setExecutable(java);

        try (MockedStatic<OperatingSystemPredicates> operatingSystemUtils = mockStatic(OperatingSystemPredicates.class,
                Mockito.CALLS_REAL_METHODS)) {
            operatingSystemUtils.when(OperatingSystemPredicates::isMacOs).thenReturn(true);

            assertThat(resolveJavaHomePath(javaBin.toString())).isEqualTo(javaHome);
        }
    }

    @Test
    void testResolveJavaHomePath_MacOsJavaStub() throws IOException {
        Path javaBin = Files.createDirectories(tempDir.resolve("bin"));
        Path java = spy(Files.createFile(javaBin.resolve(javaExecutableName())));
        FileSteps.setExecutable(java);
        doReturn(JavaHome.MACOS_JAVA_STUB).when(java).toRealPath();

        try (MockedStatic<OperatingSystemPredicates> operatingSystemUtils = mockStatic(OperatingSystemPredicates.class,
                Mockito.CALLS_REAL_METHODS); MockedStatic<Path> paths = mockStatic(Path.class, Mockito.CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(javaBin.toString(), javaExecutableName())).thenReturn(java);
            operatingSystemUtils.when(OperatingSystemPredicates::isMacOs).thenReturn(true);

            assertThat(resolveJavaHomePath(javaBin.toString())).isNull();
        }
    }

    @Test
    void testResolveJavaHomePath_RealJavaException() throws IOException {
        Path javaBin = Files.createDirectories(tempDir.resolve("bin"));
        Path java = spy(Files.createFile(javaBin.resolve(javaExecutableName())));
        FileSteps.setExecutable(java);
        doThrow(new IOException()).when(java).toRealPath();

        try (MockedStatic<Path> paths = mockStatic(Path.class, Mockito.CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(javaBin.toString(), javaExecutableName())).thenReturn(java);

            assertThat(resolveJavaHomePath(javaBin.toString())).isNull();
        }
    }

    @Test
    void testResolveJavaHomePath_MissingBinDir() throws IOException {
        Path javaBin = Files.createDirectories(tempDir.resolve("bin"));
        Path java = spy(Files.createFile(javaBin.resolve(javaExecutableName())));
        FileSteps.setExecutable(java);
        Path realJava = mock();
        doReturn(realJava).when(java).toRealPath();
        doReturn(null).when(realJava).getParent();

        try (MockedStatic<Path> paths = mockStatic(Path.class, Mockito.CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(javaBin.toString(), javaExecutableName())).thenReturn(java);

            assertThat(resolveJavaHomePath(javaBin.toString())).isNull();
        }
    }

    @Test
    void testResolveJavaHomePath_MissingHomeDir() throws IOException {
        Path javaBin = Files.createDirectories(tempDir.resolve("bin"));
        Path java = spy(Files.createFile(javaBin.resolve(javaExecutableName())));
        FileSteps.setExecutable(java);
        Path realJava = mock();
        Path bin = mock();
        doReturn(realJava).when(java).toRealPath();
        doReturn(bin).when(realJava).getParent();
        doReturn(null).when(bin).getParent();

        try (MockedStatic<Path> paths = mockStatic(Path.class, Mockito.CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(javaBin.toString(), javaExecutableName())).thenReturn(java);

            assertThat(resolveJavaHomePath(javaBin.toString())).isNull();
        }
    }

    @Test
    void testResolveJavaHomePath_MissingHomeFileName() throws IOException {
        Path javaBin = Files.createDirectories(tempDir.resolve("bin"));
        Path java = spy(Files.createFile(javaBin.resolve(javaExecutableName())));
        FileSteps.setExecutable(java);
        Path realJava = mock();
        Path bin = mock();
        Path home = mock();
        doReturn(realJava).when(java).toRealPath();
        doReturn(bin).when(realJava).getParent();
        doReturn(home).when(bin).getParent();
        doReturn(null).when(home).getFileName();

        try (MockedStatic<Path> paths = mockStatic(Path.class, Mockito.CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(javaBin.toString(), javaExecutableName())).thenReturn(java);

            assertThat(resolveJavaHomePath(javaBin.toString())).isEqualTo(home);
        }
    }

    @Test
    void testResolveJavaHomePath_Jdk8JrePath_MissingHomeDir() throws IOException {
        Path javaBin = Files.createDirectories(tempDir.resolve("bin"));
        Path java = spy(Files.createFile(javaBin.resolve(javaExecutableName())));
        FileSteps.setExecutable(java);
        Path realJava = mock();
        Path bin = mock();
        Path jre = mock();
        Path jreName = mock();
        doReturn(realJava).when(java).toRealPath();
        doReturn(bin).when(realJava).getParent();
        doReturn(jre).when(bin).getParent();
        doReturn(jreName).when(jre).getFileName();
        doReturn("jre").when(jreName).toString();
        doReturn(null).when(jre).getParent();

        try (MockedStatic<Path> paths = mockStatic(Path.class, Mockito.CALLS_REAL_METHODS)) {
            paths.when(() -> Path.of(javaBin.toString(), javaExecutableName())).thenReturn(java);

            assertThat(resolveJavaHomePath(javaBin.toString())).isEqualTo(jre);
        }
    }

    private String javaExecutableName() {
        return OperatingSystemPredicates.isWindows() ? JavaHome.JAVA_EXEC_WINDOWS : JavaHome.JAVA_EXEC;
    }

    private @Nullable Path resolveJavaHomePath(@Nullable String pathEnv) {
        try (MockedStatic<JavaHome> javaHome = mockStatic(JavaHome.class, Mockito.CALLS_REAL_METHODS)) {
            javaHome.when(JavaHome::pathEnv).thenReturn(pathEnv);
            return JavaHome.resolveJavaHomePath();
        }
    }

}
