/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.crypto.checksum;

import static com.jdheim.toolfetch.builder.TestToolBuilder.tool;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.model.tool.checksums.Checksums;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// OOC Tests for [FileChecksumService]
class FileChecksumServiceTest {

    @TempDir
    Path tempDir;

    FileChecksumService checksumService;

    TestLogListAppenderSteps testLogListAppenderSteps;

    @BeforeEach
    void setUp() {
        checksumService = new FileChecksumService();
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
    }

    @Test
    void testVerifyLogsExceptionAtDebugLevel() {
        Tool tool = tool().id("test").url("https://example.com/tool").checksums(new Checksums(Map.of("sha256", "abc"))).build();
        Path missingFile = tempDir.resolve("missing");

        assertThat(checksumService.verify(tool, missingFile)).isFalse();

        testLogListAppenderSteps.assertAnyMatch(LogLevel.WARN.toString(),
                "Checksum verification failed: \"java.io.UncheckedIOException: java.nio.file.NoSuchFileException: %s\"".formatted(missingFile));
        testLogListAppenderSteps.assertExceptionLogged(UncheckedIOException.class);
    }

}
