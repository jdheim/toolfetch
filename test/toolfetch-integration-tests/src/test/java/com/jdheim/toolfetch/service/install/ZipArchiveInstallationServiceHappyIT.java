/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.step.archive.ArchiveSteps;
import com.jdheim.toolfetch.step.assertion.AssertionSteps;
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
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/zip/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            AssertionSteps.assertSample1Archive(destinationPath);
            Path backupPath = tempDir.resolve("toolfetch.bak");
            assertThat(backupPath).doesNotExist();
            getTestLogListAppenderSteps().assertNoMatch(LogLevel.INFO.toString(),
                    "Backup path already exists: %s. Removing".formatted(backupPath));
            getTestLogListAppenderSteps().assertNoMatch(LogLevel.INFO.toString(),
                    "Destination path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
            getTestLogListAppenderSteps().assertNoMatch(LogLevel.INFO.toString(), "Removing " + backupPath);
            getTestLogListAppenderSteps().assertNoMatch(LogLevel.INFO.toString(),
                    "Reverting %s to %s".formatted(backupPath, destinationPath));
        });
    }

    @ParameterizedTest
    @CsvSource({".zip, "})
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/zip/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            AssertionSteps.assertSample2Archive(destinationPath);
            getTestLogListAppenderSteps().assertAnyMatch(LogLevel.INFO.toString(),
                    "Top-level directory \"test1/test11\" detected. Stripping during extraction");
        });
    }

    @ParameterizedTest
    @CsvSource({".zip, "})
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/zip/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            assertThat(destinationPath).isDirectory();
            AssertionSteps.assertSample3Archive(destinationPath);
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
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/zip/" + archiveName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            AssertionSteps.assertSample1Archive(destinationPath);
            Path cleanupTest = Path.of("cleanupTest.txt");
            assertThat(destinationPath.resolve(cleanupTest)).doesNotExist();
            Path backupPath = tempDir.resolve(id + ".bak");
            assertThat(backupPath).doesNotExist();
            if (backupPathExists) {
                getTestLogListAppenderSteps().assertAnyMatch(LogLevel.INFO.toString(),
                        "Backup path already exists: %s. Removing".formatted(backupPath));
            }
            getTestLogListAppenderSteps().assertAnyMatch(LogLevel.INFO.toString(),
                    "Destination path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
            getTestLogListAppenderSteps().assertAnyMatch(LogLevel.INFO.toString(), "Removing " + backupPath);
            getTestLogListAppenderSteps().assertNoMatch(LogLevel.INFO.toString(),
                    "Reverting %s to %s".formatted(backupPath, destinationPath));
        });
    }

}
