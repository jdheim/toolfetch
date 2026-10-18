/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/// OOC Tests for [PathHelper]
class PathHelperTest {

    @TempDir
    Path tempDir;

    TestLogListAppenderSteps testLogListAppenderSteps;

    @BeforeEach
    void setUp() {
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
    }

    @Test
    void testNotInstantiable() {
        AssertionSteps.assertNotInstantiable(PathHelper.class);
    }

    @Test
    void testDeleteFile() throws IOException {
        Path path = Files.createFile(tempDir.resolve("tool.zip"));

        PathHelper.deleteQuietly(path);

        assertThat(path).doesNotExist();
    }

    @Test
    void testDeleteDirectory() throws IOException {
        Path directory = Files.createDirectories(tempDir.resolve("tool/bin"));
        Files.createFile(directory.resolve("tool"));
        Path path = tempDir.resolve("tool");

        PathHelper.deleteQuietly(path);

        assertThat(path).doesNotExist();
    }

    @Test
    void testDeleteMissingPath() {
        Path path = tempDir.resolve("missing");

        PathHelper.deleteQuietly(path);

        assertThat(path).doesNotExist();
        assertThat(testLogListAppenderSteps.list).isEmpty();
    }

    @Test
    void testDeleteFailureIsLoggedAtDebug() throws IOException {
        Path path = Files.createFile(tempDir.resolve("tool.zip"));
        try (MockedStatic<PathUtils> paths = mockStatic()) {
            paths.when(() -> PathUtils.delete(path)).thenThrow(new IOException("Cannot delete"));

            PathHelper.deleteQuietly(path);

            assertThat(path).exists();
            testLogListAppenderSteps.assertExceptionLogged(IOException.class);
            testLogListAppenderSteps.assertAnyMatch(LogLevel.DEBUG.toString(),
                    "Cannot remove path: \"java.io.IOException: Cannot delete\". Path: \"%s\"".formatted(path));
            testLogListAppenderSteps.assertNoErrorNoWarn();
        }
    }

}
