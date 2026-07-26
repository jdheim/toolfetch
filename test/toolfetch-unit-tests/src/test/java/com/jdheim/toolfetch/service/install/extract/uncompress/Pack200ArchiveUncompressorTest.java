/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// OOC Tests for [Pack200ArchiveUncompressor]
class Pack200ArchiveUncompressorTest {

    Pack200ArchiveUncompressor uncompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uncompressor = new Pack200ArchiveUncompressor();
    }

    @Test
    void testUncompressPackGz_NullInputStream() throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(InputStream.nullInputStream())) {
            assertThatExceptionOfType(CompressorException.class).isThrownBy(() -> uncompressor.createCompressorInputStream(bis,
                    tempDir.resolve("toolfetch.pack.gz"))).withMessage("No Compressor found for the stream signature.");
        }
    }

    @Test
    void testUncompressPackXz_Unsupported() throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(InputStream.nullInputStream())) {
            BufferedInputStream bufferedCis = uncompressor.createCompressorInputStream(bis, tempDir.resolve("toolfetch.pack.xz"))
                    .getLeft();
            assertThat(bufferedCis).isEqualTo(bis);
        }
    }

}
