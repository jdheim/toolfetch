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

import java.io.IOException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class JarArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @ParameterizedTest
    @CsvSource({".jar, ", ".jar.pack, pack200", ".jar.pack.gz, gz"})
    void testInstall_FilesAtRoot(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) throws
            IOException {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/jar/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            assertThat(destinationPath).isDirectory();
            assertThat(destinationPath.resolve("META-INF")).isDirectory();
            assertThat(destinationPath.resolve("META-INF").resolve("MANIFEST.MF")).isRegularFile().hasContent("""
                    Manifest-Version: 1.0
                    Created-By: 25.0.2 (Eclipse Adoptium)
                    """);
            for (int i = 1; i <= 3; i++) {
                assertThat(destinationPath.resolve("test%d.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Hello ToolFetch %d".formatted(i));
            }
        });
    }

    @ParameterizedTest
    @CsvSource({".jar, ", ".jar.pack, pack200", ".jar.pack.gz, gz"})
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) throws
            IOException {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/jar/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            assertThat(destinationPath).isDirectory();
            assertThat(destinationPath.resolve("test1")).doesNotExist();
            assertThat(destinationPath.resolve("test11")).doesNotExist();
            assertThat(destinationPath.resolve("test111")).isDirectory();
            assertThat(destinationPath.resolve("test111/test1111")).isDirectory();
            assertThat(destinationPath.resolve("test111/test1111/test11111")).isDirectory();
            assertThat(destinationPath.resolve("test111/test1111/test11111/test111111")).isDirectory();
            assertThat(destinationPath.resolve("test222")).isDirectory();
            assertThat(destinationPath.resolve("test333")).isDirectory();
            assertThat(destinationPath.resolve("test333/test3333")).isDirectory();
            assertThat(destinationPath.resolve("test111/test1111/test11111/test11111.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 11111");
            assertThat(destinationPath.resolve("test111/test1111/test11111/test111111/test111111.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 111111");
            assertThat(destinationPath.resolve("test222/test222.txt")).isRegularFile().hasContent("Hello ToolFetch 222");
            assertThat(destinationPath.resolve("test333/test3333/test3333-1.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 3333-1");
            assertThat(destinationPath.resolve("test333/test3333/test3333-2.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 3333-2");
            assertThat(destinationPath.resolve("test333/test3333/test3333-3.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 3333-3");
        });
    }

    @ParameterizedTest
    @CsvSource({".jar, ", ".jar.pack, pack200", ".jar.pack.gz, gz"})
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName,
            WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/jar/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            assertThat(destinationPath).isDirectory();
            assertThat(destinationPath.resolve("test1")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test111")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test111/test1111")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111/test111111")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test222")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test333")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test333/test3333")).isDirectory();
            assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111/test11111.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 11111");
            assertThat(
                    destinationPath.resolve("test1/test11/test111/test1111/test11111/test111111/test111111.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 111111");
            assertThat(destinationPath.resolve("test1/test11/test222/test222.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 222");
            assertThat(destinationPath.resolve("test1/test11/test333/test3333/test3333-1.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 3333-1");
            assertThat(destinationPath.resolve("test1/test11/test333/test3333/test3333-2.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 3333-2");
            assertThat(destinationPath.resolve("test1/test11/test333/test3333/test3333-3.txt")).isRegularFile()
                    .hasContent("Hello ToolFetch 3333-3");
            assertThat(destinationPath.resolve("test4.txt")).isRegularFile().hasContent("Hello ToolFetch 4");
        });
    }

}
