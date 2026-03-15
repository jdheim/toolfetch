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
import org.junit.jupiter.api.Test;

/// Integration Tests for [ArchiveInstallationService]
/// Commands to create dump file:
/// dd if=/dev/zero of=fs.img bs=1M count=20
/// mkfs.ext2 fs.img
/// sudo mkdir /mnt/testfs
/// sudo mount -o loop fs.img /mnt/testfs
/// sudo cp test1.txt test2.txt test3.txt /mnt/testfs/
/// sudo dump -0 -z9 -f sample1.dump /mnt/testfs
/// sudo umount /mnt/testfs
@WireMockTest
class DumpArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @Test
    void testInstall_NotSupported(WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1.dump";
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/dump/" + archiveName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Extract failed due to exception: \"org.apache.commons.compress.archivers.ArchiveException: " +
                            "No Archiver found for the stream signature\". Skipping toolfetch");
            assertThat(destinationPath).doesNotExist();
        });
    }

}
