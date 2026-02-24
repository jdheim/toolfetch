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

import static com.jdheim.toolfetch.util.archive.ArchiveUtils.addZipEntryDir;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.addZipEntryFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class ZipArchiveInstallationServiceUnhappyIT extends TestCommonArchiveInstallationService {

    static Stream<Arguments> zipTestCases_ZipSlip() {
        return Stream.of(oneDirWithOneFile_ZipSlip(), oneDirWithOneFile_DirAsEntry_ZipSlip());
    }

    static Arguments oneDirWithOneFile_ZipSlip() {
        return Arguments.of("One Dir with One File - Zip Slip", (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
                    addZipEntryFile(zaos, "../test2.txt", "Install Test 2");
                }, (Consumer<Path>) ZipArchiveInstallationServiceUnhappyIT::pathAsserts_OneDirWithOneFile,
                new String[]{"toolfetch", "../test2.txt", "toolfetch/../test2.txt"});
    }

    static Arguments oneDirWithOneFile_DirAsEntry_ZipSlip() {
        return Arguments.of("One Dir with One File - Dir as Entry - ZipSlip", (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
                    addZipEntryDir(zaos, "../");
                    addZipEntryFile(zaos, "../test2.txt", "Install Test 2");
                }, (Consumer<Path>) ZipArchiveInstallationServiceUnhappyIT::pathAsserts_OneDirWithOneFile,
                new String[]{"toolfetch", "../test2.txt", "toolfetch/../test2.txt"});
    }

    private static void pathAsserts_OneDirWithOneFile(Path destinationPath) {
        assertThat(destinationPath).doesNotExist();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zipTestCases_ZipSlip")
    void testInstall_ZipSlip(String testCase, Consumer<ZipArchiveOutputStream> zipEntries, Consumer<Path> assertions,
            String[] logArgs, WireMockRuntimeInfo wmRuntimeInfo) {
        testInstall(wmRuntimeInfo, zipEntries, assertions);
        assertThat(logArgs).hasSize(3);
        getTestLogListAppender().assertAnyMatch(Level.WARN,
                "Extract failed due to exception: \"Detected Zip Slip vulnerability: \"%s\" + \"%s\" = \"%s\"\"".formatted(
                        tempDir.resolve(logArgs[0]), logArgs[1], tempDir.resolve(logArgs[2]).normalize()));
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @Test
    void testInstall_FilesAtRoot_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        List<String> files = List.of("test1.txt", "test2.txt", "test3.txt");
        testInstall_PasswordProtected(1, files, wmRuntimeInfo);
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @Test
    void testInstall_Strip_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        List<String> files = List.of("test1/test11/test111/test1111/test11111/test11111.txt",
                "test1/test11/test111/test1111/test11111/test111111/test111111.txt", "test1/test11/test222/test222.txt",
                "test1/test11/test333/test3333/test3333-1.txt", "test1/test11/test333/test3333/test3333-2.txt",
                "test1/test11/test333/test3333/test3333-3.txt");
        testInstall_PasswordProtected(2, files, wmRuntimeInfo);
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @Test
    void testInstall_FileAtRootNoStrip_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        List<String> files = List.of("test1/test11/test111/test1111/test11111/test11111.txt",
                "test1/test11/test111/test1111/test11111/test111111/test111111.txt", "test1/test11/test222/test222.txt",
                "test1/test11/test333/test3333/test3333-1.txt", "test1/test11/test333/test3333/test3333-2.txt",
                "test1/test11/test333/test3333/test3333-3.txt", "test4.txt");
        testInstall_PasswordProtected(3, files, wmRuntimeInfo);
    }

    private void testInstall_PasswordProtected(int index, List<String> files, WireMockRuntimeInfo wmRuntimeInfo) throws
            IOException {
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/sample%d-password.zip".formatted(index));

        testInstall(wmRuntimeInfo, archiveBytes, destinationPath -> {
            files.forEach(file -> getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Can't read archive entry at \"%s\". Skipping".formatted(file)));
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Removing " + tempDir.resolve("toolfetch/toolfetch.zip"));
            getTestLogListAppender().assertAnyMatch(Level.INFO,
                    "Nothing has been extracted. Removing " + tempDir.resolve("toolfetch"));
            assertThat(destinationPath).doesNotExist();
        });
    }

    /// ZIPX extended archive with advanced compression methods including LZMA, BZip2 and PPMd algorithms
    @Test
    void testInstall_NotSupported(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/not-supported.zipx");

        testInstall(wmRuntimeInfo, archiveBytes, destinationPath -> {
            assertThat(destinationPath).doesNotExist();
        });

        getTestLogListAppender().assertAnyMatch(Level.WARN,
                "Extract failed due to exception: \"No Archiver found for the stream signature\". Skipping toolfetch");
    }

}
