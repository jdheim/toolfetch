/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import com.jdheim.toolfetch.util.assertion.AssertionUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
@Disabled("TODO: Add Support")
class SevenZipArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @ParameterizedTest
    @CsvSource({
            ".7z,"
    })
    void testInstall_FilesAtRoot(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/7z/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample1Archive(destinationPath);
        });
    }

    @ParameterizedTest
    @CsvSource({
            ".7z,"
    })
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/7z/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample2Archive(destinationPath);
        });
    }

    @ParameterizedTest
    @CsvSource({
            ".7z,"
    })
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/7z/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample3Archive(destinationPath);
        });
    }

    /// Password-protected 7-ZIP file with a password: "toolfetch"
    @Test
    void testInstall_FilesAtRoot_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) {
        List<String> files = List.of("test1.txt", "test2.txt", "test3.txt");
        testInstall_PasswordProtected(1, files, wmRuntimeInfo);
    }

    /// Password-protected 7-ZIP file with a password: "toolfetch"
    @Test
    void testInstall_Strip_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) {
        List<String> files = List.of("test1/test11/test111/test1111/test11111/test11111.txt",
                "test1/test11/test111/test1111/test11111/test111111/test111111.txt", "test1/test11/test222/test222.txt",
                "test1/test11/test333/test3333/test3333-1.txt", "test1/test11/test333/test3333/test3333-2.txt",
                "test1/test11/test333/test3333/test3333-3.txt");
        testInstall_PasswordProtected(2, files, wmRuntimeInfo);
    }

    /// Password-protected 7-ZIP file with a password: "toolfetch"
    @Test
    void testInstall_FileAtRootNoStrip_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) {
        List<String> files = List.of("test1/test11/test111/test1111/test11111/test11111.txt",
                "test1/test11/test111/test1111/test11111/test111111/test111111.txt", "test1/test11/test222/test222.txt",
                "test1/test11/test333/test3333/test3333-1.txt", "test1/test11/test333/test3333/test3333-2.txt",
                "test1/test11/test333/test3333/test3333-3.txt", "test4.txt");
        testInstall_PasswordProtected(3, files, wmRuntimeInfo);
    }

    private void testInstall_PasswordProtected(int index, List<String> files, WireMockRuntimeInfo wmRuntimeInfo) {
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/7z/sample%d-password.7z".formatted(index));

        testInstall(wmRuntimeInfo, archiveBytes, destinationPath -> {
            files.forEach(file -> getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Can't read archive entry at \"%s\". Skipping".formatted(file)));
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Removing " + tempDir.resolve("toolfetch/toolfetch.7z"));
            getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Nothing has been extracted. Removing " + tempDir.resolve("toolfetch"));
            assertThat(destinationPath).doesNotExist();
        });
    }

}
