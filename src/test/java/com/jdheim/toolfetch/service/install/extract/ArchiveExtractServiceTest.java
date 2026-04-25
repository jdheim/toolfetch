/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// OOC Tests for [ArchiveExtractService]
class ArchiveExtractServiceTest {

    ArchiveExtractService archiveExtractService;

    TestLogListAppender testLogListAppender;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        archiveExtractService = new ArchiveExtractService();
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(ArchiveExtractService.class);
    }

    @Test
    void testExtract_MissingDestinationPath() {
        Tool tool = new Tool("toolfetch", "http://localhost/download/toolfetch.zip");
        Configuration configuration = new Configuration(tempDir.toString(), List.of(tool));
        archiveExtractService.extract(configuration, tool, Path.of("toolfetch.zip"));

        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppender.assertAnyMatch(Level.WARN, "Destination Path could not be resolved. Skipping " + tool.id());
    }

    @Test
    void testExtractEntry_MissingTargetParent_ZipSlip() throws IOException {
        Path target = Path.of("toolfetch.txt");
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            ArchiveEntry archiveEntry = mock();
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                            () -> archiveExtractService.extractEntry(acis, archiveEntry, target, 0))
                    .withMessage(
                            "Detected Zip Slip vulnerability: Archive Entry \"toolfetch.txt\" does not have parent directory");
        }
    }

    @Test
    void testExtractEntry_TargetExists() throws IOException {
        Path target = tempDir.resolve("test1.txt");
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            ArchiveEntry archiveEntry = mock();
            archiveExtractService.extractEntry(acis, archiveEntry, target, 0);
            assertThat(target).isEmptyFile();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "test, '', test", "test/a/, 'test/a/b/', ''", "test/a/b/, 'test/a/', 'b/'", "test/a/b/c/d/e/, 'test/a/', 'b/c/d/e/'",
            "test/a/b/c/d/e/, 'a/b/', 'test/a/b/c/d/e/'"
    })
    void testStripEntryName(String actualEntryName, String topLevelDir, String expectedEntryName) {
        ArchiveEntry archiveEntry = new ZipArchiveEntry(actualEntryName);
        String strippedEntryName = archiveExtractService.stripEntryName(archiveEntry, topLevelDir);
        assertThat(strippedEntryName).isEqualTo(expectedEntryName);
    }

    @ParameterizedTest
    @CsvSource({
            ", ", "'', ''", "test, test/", "test/, test/"
    })
    void testNormalize(String actualEntryName, String expectedEntryName) {
        String normalizedEntryName = archiveExtractService.normalize(actualEntryName);
        assertThat(normalizedEntryName).isEqualTo(expectedEntryName);
    }

}
