/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.step.archive.ArchiveSteps;
import org.junit.jupiter.api.Test;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class ArArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @Test
    void testInstall_NotSupported(WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1.ar";
        byte[] archiveBytes = ArchiveSteps.readTestFile("/archive/ar/" + archiveName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppenderSteps().assertAnyMatch(Level.WARN,
                    "Extract failed due to exception: \"org.apache.commons.compress.archivers.ArchiveException: "
                            + "No Archiver found for the stream signature\". Skipping toolfetch");
            assertThat(destinationPath).doesNotExist();
        });
    }

}
