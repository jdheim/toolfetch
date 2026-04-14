/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.junit.jupiter.api.Test;

/// OOC Tests for [ArchiveWithCompressorInputStream]
class ArchiveWithCompressorInputStreamTest {

    @Test
    void testCompressedUncompressedCount_Ais() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) ais;
            when(streamStatistics.getCompressedCount()).thenReturn(2L * 1024 * 1024);
            when(streamStatistics.getUncompressedCount()).thenReturn(200L * 1024 * 1024);
            assertThat(acis.getCompressedCount()).isEqualTo(2L * 1024 * 1024);
            assertThat(acis.getUncompressedCount()).isEqualTo(200L * 1024 * 1024);
        }
    }

    @Test
    void testCompressedUncompressedCount_LastCis() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             CompressorInputStream lastCis = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, lastCis)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) lastCis;
            when(streamStatistics.getCompressedCount()).thenReturn(2L * 1024 * 1024);
            when(streamStatistics.getUncompressedCount()).thenReturn(200L * 1024 * 1024);
            assertThat(acis.getCompressedCount()).isEqualTo(2L * 1024 * 1024);
            assertThat(acis.getUncompressedCount()).isEqualTo(200L * 1024 * 1024);
        }
    }

    @Test
    void testCompressedUncompressedCount_NoInputStreamStatistics() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             CompressorInputStream lastCis = mock();
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, lastCis)) {
            assertThat(acis.getCompressedCount()).isZero();
            assertThat(acis.getUncompressedCount()).isZero();
        }
    }

}
