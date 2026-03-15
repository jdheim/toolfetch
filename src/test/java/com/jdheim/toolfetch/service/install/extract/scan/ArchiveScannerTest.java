/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// OOC Tests for [ArchiveScanner]
class ArchiveScannerTest {

    ArchiveScanner archiveScanner;

    @BeforeEach
    void setUp() {
        archiveScanner = new ArchiveScanner();
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
