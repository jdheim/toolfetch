/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import com.jdheim.toolfetch.util.assertion.AssertionUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class ZipArchiveInstallationServiceHappyIT extends TestCommonArchiveInstallationService {

    @ParameterizedTest
    @CsvSource({".zip, "})
    void testInstall_FilesAtRoot(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample1Archive(destinationPath);
            Path backupPath = tempDir.resolve("toolfetch.bak");
            assertThat(backupPath).doesNotExist();
            getTestLogListAppender().assertNoMatch(Level.INFO, "Backup Path already exists: %s. Removing".formatted(backupPath));
            getTestLogListAppender().assertNoMatch(Level.INFO,
                    "Destination Path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
            getTestLogListAppender().assertNoMatch(Level.INFO, "Removing " + backupPath);
            getTestLogListAppender().assertNoMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
        });
    }

    @ParameterizedTest
    @CsvSource({".zip, "})
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample2Archive(destinationPath);
            getTestLogListAppender().assertAnyMatch(Level.INFO,
                    "Top-level directory \"test1/test11\" detected. Stripping during extraction");
        });
    }

    @ParameterizedTest
    @CsvSource({".zip, "})
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            assertThat(destinationPath).isDirectory();
            AssertionUtils.assertSample3Archive(destinationPath);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInstall_Cleanup(boolean backupPathExists, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String id = "toolfetch";
        fillDestinationPath(id);
        if (backupPathExists) {
            fillBackupPath(id);
        }
        String archiveName = "sample1.zip";
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/" + archiveName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample1Archive(destinationPath);
            Path cleanupTest = Path.of("cleanupTest.txt");
            assertThat(destinationPath.resolve(cleanupTest)).doesNotExist();
            Path backupPath = tempDir.resolve(id + ".bak");
            assertThat(backupPath).doesNotExist();
            if (backupPathExists) {
                getTestLogListAppender().assertAnyMatch(Level.INFO,
                        "Backup Path already exists: %s. Removing".formatted(backupPath));
            }
            getTestLogListAppender().assertAnyMatch(Level.INFO,
                    "Destination Path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Removing " + backupPath);
            getTestLogListAppender().assertNoMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
        });
    }

}
