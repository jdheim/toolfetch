/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jdheim.toolfetch.step.archive.ArchiveSteps;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Component Tests for [ArchiveScanner]
class ArchiveScannerTest {

    ArchiveScanner archiveScanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        archiveScanner = new ArchiveScanner();
    }

    @Test
    void testScan_CollectsZipModesAfterRootEntry() throws IOException {
        Path archive = Files.write(tempDir.resolve("sample1.zip"), ArchiveSteps.readTestFile("/archive/zip/sample1.zip"));

        ScanResult result = archiveScanner.scan(archive);

        assertThat(result.zipModes()).hasSize(1)
                .containsEntry("test1-link.txt", UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
    }

    @ParameterizedTest
    @CsvSource({"755, true", "654, true", "644, false", "444, false", "645, false", "0, false"})
    void testScan_RetainsOnlyExecutableZipFileModes(String octalPermissions, boolean executable) throws IOException {
        Path archive = tempDir.resolve("tool.zip");
        int mode = UnixStat.FILE_FLAG | Integer.parseInt(octalPermissions, 8);
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry entry = new ZipArchiveEntry("tool");
            entry.setUnixMode(mode);
            out.putArchiveEntry(entry);
            out.write(1);
            out.closeArchiveEntry();
        }

        ScanResult result = archiveScanner.scan(archive);

        if (executable) {
            assertThat(result.zipModes()).containsEntry("tool", mode);
        } else {
            assertThat(result.zipModes()).doesNotContainKey("tool");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "true, /tmp, /tmp", "false, /tmp/test1, /tmp/test2", "false, /tmp, /opt", "false, /tmp/test1, /opt/test2",
            "true, /, /tmp", "false, /tmp, /", "true, /, /"
    })
    void testIsWithinTopLevelDir(String expected, String topLevelDir, String archiveEntryParent) {
        boolean matches = archiveScanner.isWithinTopLevelDir(Path.of(topLevelDir), Path.of(archiveEntryParent));
        assertThat(matches).isEqualTo(Boolean.parseBoolean(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "/tmp, /tmp, /tmp", "/tmp, /tmp/test1, /tmp/test2", "null, /tmp, /opt", "null, /tmp/test1, /opt/test2",
            "null, /, /tmp", "/, /tmp, /", "/, /, /", "null, /tmp, null"
    })
    void testGetCommonParent(String expected, String topLevelDir, String archiveEntryParent) {
        Path actualArchiveEntryParent = "null".equals(archiveEntryParent) ? null : Path.of(archiveEntryParent);
        Path commonParent = archiveScanner.getCommonParent(Path.of(topLevelDir), actualArchiveEntryParent);
        Path expectedPath = "null".equals(expected) ? null : Path.of(expected);
        assertThat(commonParent).isEqualTo(expectedPath);
    }

    @ParameterizedTest
    @CsvSource({
            "true, /tmp, /tmp", "false, /tmp/test1, /tmp/test2", "false, /tmp, /opt", "false, /tmp/test1, /opt/test2",
            "false, /, /tmp", "true, /tmp, /", "true, /, /"
    })
    void testIsAncestorOfTopLevelDir(String expected, String topLevelDir, String archiveEntryParent) {
        boolean matches = archiveScanner.isAncestorOfTopLevelDir(Path.of(topLevelDir), Path.of(archiveEntryParent));
        assertThat(matches).isEqualTo(Boolean.parseBoolean(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "true, /tmp, /tmp", "true, /tmp/test1, /tmp/test2", "false, /tmp, /opt", "false, /tmp/test1, /opt/test2",
            "false, /, /tmp", "false, /tmp, /", "false, /, /"
    })
    void testMatchesFirstSegmentOfTopLevelDir(String expected, String topLevelDir, String archiveEntryParent) {
        boolean matches = archiveScanner.matchesFirstSegmentOfTopLevelDir(Path.of(topLevelDir), Path.of(archiveEntryParent));
        assertThat(matches).isEqualTo(Boolean.parseBoolean(expected));
    }

}
