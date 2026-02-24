/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
