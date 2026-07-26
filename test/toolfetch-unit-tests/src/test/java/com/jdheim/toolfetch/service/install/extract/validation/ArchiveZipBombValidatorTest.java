/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.validation;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.junit.jupiter.api.Test;

/// OOC Tests for [ArchiveZipBombValidator]
class ArchiveZipBombValidatorTest {

    @Test
    void testValidateEntryCount_Valid() {
        assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateEntryCount(10_000));
    }

    @Test
    void testValidateEntryCount_Invalid() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                        () -> ArchiveZipBombValidator.validateEntryCount(10_001))
                .withMessage("Detected Zip Bomb vulnerability: Archive contains more than 10000 entries");
    }

    @Test
    void testValidateTotalSize_Valid() {
        assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateTotalSize(10L * 1024 * 1024 * 1024));
    }

    @Test
    void testValidateTotalSize_Invalid() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                        () -> ArchiveZipBombValidator.validateTotalSize(10L * 1024 * 1024 * 1024 + 1))
                .withMessage("Detected Zip Bomb vulnerability: Archive is larger than 10 GB when extracted");
    }

    @Test
    void testValidateCompressionRatio_Ais_Zero() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) ais;
            when(streamStatistics.getCompressedCount()).thenReturn(0L);
            when(streamStatistics.getUncompressedCount()).thenReturn(0L);
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry));
        }
    }

    @Test
    void testValidateCompressionRatio_Ais_Valid() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) ais;
            when(streamStatistics.getCompressedCount()).thenReturn(2L * 1024 * 1024);
            when(streamStatistics.getUncompressedCount()).thenReturn(200L * 1024 * 1024);
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry));
        }
    }

    @Test
    void testValidateCompressionRatio_Ais_Invalid() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) ais;
            when(streamStatistics.getCompressedCount()).thenReturn(2L * 1024 * 1024);
            when(streamStatistics.getUncompressedCount()).thenReturn(200L * 1024 * 1024 + 1);
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                            () -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry))
                    .withMessage(
                            "Detected Zip Bomb vulnerability: Archive Entry \"test/test.txt\" has suspicious compression ratio \"100.00\" (compressed size = 2 MB, extracted size = 200 MB)");
        }
    }

    @Test
    void testValidateCompressionRatio_LastCis_Zero() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             CompressorInputStream lastCis = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, lastCis)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) lastCis;
            when(streamStatistics.getCompressedCount()).thenReturn(0L);
            when(streamStatistics.getUncompressedCount()).thenReturn(0L);
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry));
        }
    }

    @Test
    void testValidateCompressionRatio_LastCis_Valid() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             CompressorInputStream lastCis = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, lastCis)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) lastCis;
            when(streamStatistics.getCompressedCount()).thenReturn(2L * 1024 * 1024);
            when(streamStatistics.getUncompressedCount()).thenReturn(200L * 1024 * 1024);
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry));
        }
    }

    @Test
    void testValidateCompressionRatio_LastCis_Invalid() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             CompressorInputStream lastCis = mock(withSettings().extraInterfaces(InputStreamStatistics.class));
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, lastCis)) {
            InputStreamStatistics streamStatistics = (InputStreamStatistics) lastCis;
            when(streamStatistics.getCompressedCount()).thenReturn(2L * 1024 * 1024);
            when(streamStatistics.getUncompressedCount()).thenReturn(200L * 1024 * 1024 + 1);
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                            () -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry))
                    .withMessage(
                            "Detected Zip Bomb vulnerability: Archive Entry \"test/test.txt\" has suspicious compression ratio \"100.00\" (compressed size = 2 MB, extracted size = 200 MB)");
        }
    }

    @Test
    void testValidateCompressionRatio_NoInputStreamStatistics() throws IOException {
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             CompressorInputStream lastCis = mock();
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, lastCis)) {
            ZipArchiveEntry archiveEntry = mock();
            when(archiveEntry.toString()).thenReturn("test/test.txt");
            assertThatNoException().isThrownBy(() -> ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry));
        }
    }

    @Test
    void testNoInstance() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(ArchiveZipBombValidator::new);
    }

}
