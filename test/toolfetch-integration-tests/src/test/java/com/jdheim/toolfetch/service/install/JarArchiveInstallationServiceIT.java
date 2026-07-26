/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.step.archive.ArchiveSteps;
import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class JarArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @ParameterizedTest
    @CsvSource({".jar, ", ".jar.pack, pack200", ".jar.pack.gz, gz"})
    void testInstall_FilesAtRoot(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/jar/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            AssertionSteps.assertSample1Archive(destinationPath);
            assertThat(destinationPath.resolve("META-INF")).isDirectory();
            assertThat(destinationPath.resolve("META-INF").resolve("MANIFEST.MF")).isRegularFile().hasContent("""
                    Manifest-Version: 1.0
                    Created-By: 25.0.2 (Eclipse Adoptium)
                    """);
        });
    }

    @ParameterizedTest
    @CsvSource({".jar, ", ".jar.pack, pack200", ".jar.pack.gz, gz"})
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/jar/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            AssertionSteps.assertSample2Archive(destinationPath);
        });
    }

    @ParameterizedTest
    @CsvSource({".jar, ", ".jar.pack, pack200", ".jar.pack.gz, gz"})
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/jar/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertNoErrorNoWarn();
            AssertionSteps.assertSample3Archive(destinationPath);
        });
    }

}
