/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// OOC Tests for [CompositeArchiveUncompressor]
class CompositeArchiveUncompressorTest {

    CompositeArchiveUncompressor uncompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uncompressor = new CompositeArchiveUncompressor();
    }

    @Test
    void testUncompress_ArchiveException() {
        AutoDetectArchiveUncompressor autoDetectArchiveUncompressor = mock();
        when(autoDetectArchiveUncompressor.isApplicable(any())).thenReturn(false);
        List<Uncompressor> uncompressors = List.of(autoDetectArchiveUncompressor);
        CompositeArchiveUncompressor spyUncompressor = spy(uncompressor);
        doReturn(uncompressors).when(spyUncompressor).getUncompressors();

        assertThatExceptionOfType(ArchiveException.class).isThrownBy(() -> {
            try (var _ = spyUncompressor.uncompress(tempDir.resolve("toolfetch.zip"))) {
                throw new AssertionError("ArchiveException is expected");
            }
        }).withMessage("No Archiver found for the stream signature");
    }

}
