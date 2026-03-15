/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Integration Tests for [ArchiveInstallationService]
/// Commands to create:
/// Bin (legacy) format: find test1 test4.txt -print | cpio -o > sample3.cpio
/// Newc format: find test1 test4.txt -print | cpio -o -H newc > sample3-newc.cpio
@WireMockTest
class CpioArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @ParameterizedTest
    @CsvSource({
            ".cpio,", ".cpio.gz, gz", "-newc.cpio,", "-newc.cpio.gz, gz"
    })
    void testInstall_NotSupported(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/cpio/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Extract failed due to exception: \"org.apache.commons.compress.archivers.ArchiveException: " +
                            "No Archiver found for the stream signature\". Skipping toolfetch");
            assertThat(destinationPath).doesNotExist();
        });
    }

}
