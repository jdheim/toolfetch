/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/// OOC Tests for [BrotliArchiveUncompressor]
class BrotliArchiveUncompressorTest {

    BrotliArchiveUncompressor uncompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uncompressor = new BrotliArchiveUncompressor();
    }

    @Test
    void testUncompress_CompressorException() throws IOException {
        CompressorStreamFactory compressorStreamFactory = mock();
        when(compressorStreamFactory.createCompressorInputStream(eq(CompressorStreamFactory.BROTLI), any())).thenThrow(
                new CompressorException("Brotli compression is not available"));
        try (MockedStatic<AutoDetectArchiveUncompressor> autoDetectArchiveUncompressor = mockStatic(
                AutoDetectArchiveUncompressor.class, CALLS_REAL_METHODS);
             BufferedInputStream bis = new BufferedInputStream(InputStream.nullInputStream())) {
            autoDetectArchiveUncompressor.when(AutoDetectArchiveUncompressor::compressorStreamFactory)
                    .thenReturn(compressorStreamFactory);

            assertThatExceptionOfType(CompressorException.class).isThrownBy(
                    () -> uncompressor.createCompressorInputStream(bis, tempDir.resolve("toolfetch.br")));
        }
    }

}
