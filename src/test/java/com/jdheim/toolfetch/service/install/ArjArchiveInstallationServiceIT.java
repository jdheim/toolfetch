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

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import org.junit.jupiter.api.Test;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class ArjArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @Test
    void testInstall_NotSupported(WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1.arj";
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/arj/" + archiveName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Extract failed due to exception: \"org.apache.commons.compress.archivers.ArchiveException: " +
                            "No Archiver found for the stream signature\". Skipping toolfetch");
            assertThat(destinationPath).doesNotExist();
        });
    }

}
