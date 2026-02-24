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

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class ZipLevel2ArchiveInstallationServiceHappyIT extends TestCommonArchiveInstallationService {

    static Stream<Arguments> zipTestCases() {
        return Stream.of(oneDirWithOneFile(), oneDirWithOneFile_DirAsEntry(), oneDirWithOneFileAndFileAtRoot(),
                oneDirWithOneFileAndFileAtRoot_DirAsEntry(), threeDirsWithTwoFilesAtLevel1To2(),
                threeDirsWithTwoFilesAtLevel1To2_DirAsEntry());
    }

    static Arguments oneDirWithOneFile() {
        return Arguments.of("One Dir with One File",
                (Consumer<ZipArchiveOutputStream>) zaos -> addZipEntryFile(zaos, "test1/test2/test2.txt", "Install Test 2"),
                (Consumer<Path>) ZipLevel2ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile);
    }

    static Arguments oneDirWithOneFile_DirAsEntry() {
        return Arguments.of("One Dir with One File - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryFile(zaos, "test1/test2/test2.txt", "Install Test 2");
        }, (Consumer<Path>) ZipLevel2ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile);
    }

    private static void pathAsserts_OneDirWithOneFile(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).doesNotExist();
        assertThat(destinationPath.resolve("test1/test2")).doesNotExist();
        assertThat(destinationPath.resolve("test2.txt")).isRegularFile().hasContent("Install Test 2");
    }

    static Arguments oneDirWithOneFileAndFileAtRoot() {
        return Arguments.of("One Dir with One File and File at Root", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test2.txt", "Install Test 2");
            addZipEntryFile(zaos, "test3.txt", "Install Test 3");
        }, (Consumer<Path>) ZipLevel2ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFileAndFileAtRoot);
    }

    static Arguments oneDirWithOneFileAndFileAtRoot_DirAsEntry() {
        return Arguments.of("One Dir with One File and File at Root - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2");
            addZipEntryFile(zaos, "test1/test2/test2.txt", "Install Test 2");
            addZipEntryFile(zaos, "test3.txt", "Install Test 3");
        }, (Consumer<Path>) ZipLevel2ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFileAndFileAtRoot);
    }

    private static void pathAsserts_OneDirWithOneFileAndFileAtRoot(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2/test2.txt")).isRegularFile().hasContent("Install Test 2");
        assertThat(destinationPath.resolve("test3.txt")).isRegularFile().hasContent("Install Test 3");
    }

    static Arguments threeDirsWithTwoFilesAtLevel1To2() {
        return Arguments.of("Three Dirs with Two Files at Level 1 to 2", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test3/test32.txt", "Install Test 32");
            addZipEntryFile(zaos, "test1/test3/test31.txt", "Install Test 31");
            addZipEntryFile(zaos, "test1/test2/test22.txt", "Install Test 22");
            addZipEntryFile(zaos, "test1/test2/test21.txt", "Install Test 21");
            addZipEntryFile(zaos, "test1/test12.txt", "Install Test 12");
            addZipEntryFile(zaos, "test1/test11.txt", "Install Test 11");
        }, (Consumer<Path>) ZipLevel2ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithTwoFilesAtLevel1To2);
    }

    static Arguments threeDirsWithTwoFilesAtLevel1To2_DirAsEntry() {
        return Arguments.of("Three Dirs with Two Files at Level 1 to 2 - Dir as Entry",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/test3");
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryFile(zaos, "test1/test3/test32.txt", "Install Test 32");
                    addZipEntryFile(zaos, "test1/test3/test31.txt", "Install Test 31");
                    addZipEntryFile(zaos, "test1/test2/test22.txt", "Install Test 22");
                    addZipEntryFile(zaos, "test1/test2/test21.txt", "Install Test 21");
                    addZipEntryFile(zaos, "test1/test12.txt", "Install Test 12");
                    addZipEntryFile(zaos, "test1/test11.txt", "Install Test 11");
                }, (Consumer<Path>) ZipLevel2ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithTwoFilesAtLevel1To2);
    }

    private static void pathAsserts_ThreeDirsWithTwoFilesAtLevel1To2(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).isDirectory();
        assertThat(destinationPath.resolve("test3")).isDirectory();
        assertThat(destinationPath.resolve("test11.txt")).isRegularFile().hasContent("Install Test 11");
        assertThat(destinationPath.resolve("test12.txt")).isRegularFile().hasContent("Install Test 12");
        assertThat(destinationPath.resolve("test2/test21.txt")).isRegularFile().hasContent("Install Test 21");
        assertThat(destinationPath.resolve("test2/test22.txt")).isRegularFile().hasContent("Install Test 22");
        assertThat(destinationPath.resolve("test3/test31.txt")).isRegularFile().hasContent("Install Test 31");
        assertThat(destinationPath.resolve("test3/test32.txt")).isRegularFile().hasContent("Install Test 32");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zipTestCases")
    void testInstall(String testCase, Consumer<ZipArchiveOutputStream> zipEntries, Consumer<Path> assertions,
            WireMockRuntimeInfo wmRuntimeInfo) {
        testInstall(wmRuntimeInfo, zipEntries, assertions);
    }

}
