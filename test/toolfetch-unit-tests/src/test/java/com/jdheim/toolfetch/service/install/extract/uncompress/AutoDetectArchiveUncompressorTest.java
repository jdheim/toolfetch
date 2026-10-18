/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jdheim.toolfetch.service.exception.SkippedArchiveException;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/// OOC Tests for [AutoDetectArchiveUncompressor]
class AutoDetectArchiveUncompressorTest {

    AutoDetectArchiveUncompressor uncompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uncompressor = new AutoDetectArchiveUncompressor();
    }

    @Test
    void testBinaryDetectionLogsOriginalExceptionAtDebug() throws IOException {
        Path binary = Files.createFile(tempDir.resolve("tool.bin"));
        TestLogListAppenderSteps logs = new TestLogListAppenderSteps();
        logs.start();

        assertThatExceptionOfType(SkippedArchiveException.class).isThrownBy(() -> uncompressor.uncompress(binary));

        logs.assertExceptionLogged(ArchiveException.class);
        logs.assertNoErrorNoWarn();
    }

    @Test
    void testUncompress_ArchiveException() throws IOException {
        Path archivePath = Files.createFile(tempDir.resolve("toolfetch.archive"));
        try (MockedStatic<ArchiveStreamFactory> archiveStreamFactory = mockStatic(ArchiveStreamFactory.class)) {
            archiveStreamFactory.when(() -> ArchiveStreamFactory.detect(any()))
                    .thenThrow(new ArchiveException("Archive detection failed"));

            assertThatExceptionOfType(ArchiveException.class).isThrownBy(() -> {
                try (ArchiveWithCompressorInputStream _ = uncompressor.uncompress(archivePath)) {
                    throw new AssertionError("ArchiveException is expected");
                }
            }).withMessage("Archive detection failed");
        }
    }

}
