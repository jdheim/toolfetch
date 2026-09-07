/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/// OOC Tests for [OsRelease]
class OsReleaseTest {

    @TempDir
    Path tempDir;

    @Test
    void testNotInstantiable() {
        AssertionSteps.assertNotInstantiable(OsRelease.class);
    }

    @Test
    void testLazyHolderNotInstantiable() {
        AssertionSteps.assertNotInstantiable(OsRelease.LazyHolder.class);
    }

    @Test
    void testOsReleasePath() {
        assertThat(OsRelease.osReleasePath()).isEqualTo(Path.of("/etc/os-release"));
    }

    @Test
    void testParseOsRelease() {
        Map<String, String> osRelease = OsRelease.get();
        assertThat(osRelease).isNotNull();
        assertThat(osRelease.get(OsRelease.PRETTY_NAME)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.NAME)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.VERSION_ID)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.VERSION)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.VERSION_CODENAME)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.ID)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.ID_LIKE)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.HOME_URL)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.SUPPORT_URL)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.BUG_REPORT_URL)).isNotEmpty();
        assertThat(osRelease.get(OsRelease.PRIVACY_POLICY_URL)).isNotEmpty();
    }

    @Test
    void testParseOsRelease_Unreadable() {
        assertThat(parseOsRelease(tempDir.resolve("missing"))).isEmpty();
    }

    @Test
    void testParseOsRelease_ReadException() throws IOException {
        Path readableOsReleasePath = Files.createFile(tempDir.resolve("os-release"));
        Path missingOsReleasePath = tempDir.resolve("missing");
        try (MockedStatic<OsRelease> osRelease = mockStatic(OsRelease.class, Mockito.CALLS_REAL_METHODS)) {
            osRelease.when(OsRelease::osReleasePath).thenReturn(readableOsReleasePath, missingOsReleasePath);

            assertThat(OsRelease.parseOsRelease()).isEmpty();
        }
    }

    @Test
    void testParseOsRelease_Custom() throws IOException {
        Path osRelease = tempDir.resolve("os-release");
        Files.writeString(osRelease, """
                
                \s\s\s
                # comment
                  # indented comment
                missing separator
                 =missing key
                NAME=ToolFetch
                PRETTY_NAME="ToolFetch OS"
                ID=first
                ID=last
                EMPTY=
                """);

        assertThat(parseOsRelease(osRelease)).containsExactlyInAnyOrderEntriesOf(
                Map.of(OsRelease.NAME, "ToolFetch", OsRelease.PRETTY_NAME, "ToolFetch OS", OsRelease.ID, "last", "EMPTY", ""));
    }

    private Map<String, String> parseOsRelease(Path osReleasePath) {
        try (MockedStatic<OsRelease> osRelease = mockStatic(OsRelease.class, Mockito.CALLS_REAL_METHODS)) {
            osRelease.when(OsRelease::osReleasePath).thenReturn(osReleasePath);
            return OsRelease.parseOsRelease();
        }
    }

}
