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
    void testUncompress_ArchiveException() throws IOException {
        Path archivePath = Files.createFile(tempDir.resolve("toolfetch.archive"));
        try (MockedStatic<ArchiveStreamFactory> archiveStreamFactory = mockStatic(ArchiveStreamFactory.class)) {
            archiveStreamFactory.when(() -> ArchiveStreamFactory.detect(any()))
                    .thenThrow(new ArchiveException("Archive detection failed"));

            assertThatExceptionOfType(ArchiveException.class).isThrownBy(() -> {
                try (var _ = uncompressor.uncompress(archivePath)) {
                    throw new AssertionError("ArchiveException is expected");
                }
            }).withMessage("Archive detection failed");
        }
    }

}
