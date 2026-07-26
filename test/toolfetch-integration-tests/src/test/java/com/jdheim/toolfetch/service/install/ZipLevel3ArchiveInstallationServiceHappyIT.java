/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static com.jdheim.toolfetch.step.archive.ArchiveSteps.addZipEntryDir;
import static com.jdheim.toolfetch.step.archive.ArchiveSteps.addZipEntryFile;
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
class ZipLevel3ArchiveInstallationServiceHappyIT extends TestCommonArchiveInstallationService {

    static Stream<Arguments> zipTestCases() {
        return Stream.of(oneDirWithOneFile(), oneDirWithOneFile_DirAsEntry1(), oneDirWithOneFile_DirAsEntry2(),
                oneDirWithOneFile_DirAsEntry3(), oneDirWithOneFile_DirAsEntry4(), oneDirWithOneFile_DirAsEntry5(),
                oneDirWithOneFileAndFileAtRoot(), oneDirWithOneFileAndFileAtRoot_DirAsEntry(), twoDirsWithOneFileAtLevel1To3(),
                twoDirsWithOneFileAtLevel1To3_DirAsEntry(), twoDirsWithOneFileAtLevel2To3(),
                twoDirsWithOneFileAtLevel2To3_DirAsEntry1(), twoDirsWithOneFileAtLevel2To3_DirAsEntry2(),
                twoDirsWithOneFileAtLevel2To3_DirAsEntry3(), twoDirsWithOneFileAtLevel2To3_DirAsEntry4(),
                twoDirsWithOneFileAtLevel2To3_DirAsEntry5(), threeDirsWithOneFileAtLevel1To3(),
                threeDirsWithOneFileAtLevel1To3_DirAsEntry(), threeDirsWithTwoFilesAtLevel2To3(),
                threeDirsWithTwoFilesAtLevel2To3_DirAsEntry());
    }

    static Arguments oneDirWithOneFile() {
        return Arguments.of("One Dir with One File",
                (Consumer<ZipArchiveOutputStream>) zaos -> addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3"),
                (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile_DirAsEntry);
    }

    static Arguments oneDirWithOneFile_DirAsEntry1() {
        return Arguments.of("One Dir with One File - Dir as Entry 1", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile_DirAsEntry);
    }

    static Arguments oneDirWithOneFile_DirAsEntry2() {
        return Arguments.of("One Dir with One File - Dir as Entry 2", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryDir(zaos, "test1/test2/test3/");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile_DirAsEntry);
    }

    static Arguments oneDirWithOneFile_DirAsEntry3() {
        return Arguments.of("One Dir with One File - Dir as Entry 3", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile_DirAsEntry);
    }

    static Arguments oneDirWithOneFile_DirAsEntry4() {
        return Arguments.of("One Dir with One File - Dir as Entry 4", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryDir(zaos, "test1/");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile_DirAsEntry);
    }

    static Arguments oneDirWithOneFile_DirAsEntry5() {
        return Arguments.of("One Dir with One File - Dir as Entry 5", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryDir(zaos, "test1/");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile_DirAsEntry);
    }

    private static void pathAsserts_OneDirWithOneFile_DirAsEntry(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).doesNotExist();
        assertThat(destinationPath.resolve("test3")).doesNotExist();
        assertThat(destinationPath.resolve("test1/test2")).doesNotExist();
        assertThat(destinationPath.resolve("test1/test2/test3")).doesNotExist();
        assertThat(destinationPath.resolve("test3.txt")).isRegularFile().hasContent("Install Test 3");
    }

    static Arguments oneDirWithOneFileAndFileAtRoot() {
        return Arguments.of("One Dir with One File and File at Root", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryFile(zaos, "test4.txt", "Install Test 4");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFileAndFileAtRoot);
    }

    static Arguments oneDirWithOneFileAndFileAtRoot_DirAsEntry() {
        return Arguments.of("One Dir with One File and File at Root - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryFile(zaos, "test4.txt", "Install Test 4");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFileAndFileAtRoot);
    }

    private static void pathAsserts_OneDirWithOneFileAndFileAtRoot(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2/test3")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2/test3/test3.txt")).isRegularFile().hasContent("Install Test 3");
        assertThat(destinationPath.resolve("test4.txt")).isRegularFile().hasContent("Install Test 4");
    }

    static Arguments twoDirsWithOneFileAtLevel1To3() {
        return Arguments.of("Two Dirs with One File at Level 1 to 3", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryFile(zaos, "test1/test4.txt", "Install Test 4");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel1To3);
    }

    static Arguments twoDirsWithOneFileAtLevel1To3_DirAsEntry() {
        return Arguments.of("Two Dirs with One File at Level 1 to 3 - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryFile(zaos, "test1/test4.txt", "Install Test 4");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel1To3);
    }

    private static void pathAsserts_TwoDirsWithOneFileAtLevel1To3(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).isDirectory();
        assertThat(destinationPath.resolve("test2/test3")).isDirectory();
        assertThat(destinationPath.resolve("test2/test3/test3.txt")).isRegularFile().hasContent("Install Test 3");
        assertThat(destinationPath.resolve("test4.txt")).isRegularFile().hasContent("Install Test 4");
    }

    static Arguments twoDirsWithOneFileAtLevel2To3() {
        return Arguments.of("Two Dirs with One File at Level 2 to 3", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryFile(zaos, "test1/test2/test4.txt", "Install Test 4");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel2To3);
    }

    static Arguments twoDirsWithOneFileAtLevel2To3_DirAsEntry1() {
        return Arguments.of("Two Dirs with One File at Level 2 to 3 - Dir as Entry 1",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/test2/test3/");
                    addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
                    addZipEntryFile(zaos, "test1/test2/test4.txt", "Install Test 4");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel2To3);
    }

    static Arguments twoDirsWithOneFileAtLevel2To3_DirAsEntry2() {
        return Arguments.of("Two Dirs with One File at Level 2 to 3 - Dir as Entry 2",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/test2/test3/");
                    addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
                    addZipEntryFile(zaos, "test1/test2/test4.txt", "Install Test 4");
                    addZipEntryDir(zaos, "test1/");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel2To3);
    }

    static Arguments twoDirsWithOneFileAtLevel2To3_DirAsEntry3() {
        return Arguments.of("Two Dirs with One File at Level 2 to 3 - Dir as Entry 3",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/test2/test3/");
                    addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
                    addZipEntryFile(zaos, "test1/test2/test4.txt", "Install Test 4");
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryDir(zaos, "test1/test2/");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel2To3);
    }

    static Arguments twoDirsWithOneFileAtLevel2To3_DirAsEntry4() {
        return Arguments.of("Two Dirs with One File at Level 2 to 3 - Dir as Entry 4",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
                    addZipEntryFile(zaos, "test1/test2/test4.txt", "Install Test 4");
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/test2/test3/");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel2To3);
    }

    static Arguments twoDirsWithOneFileAtLevel2To3_DirAsEntry5() {
        return Arguments.of("Two Dirs with One File at Level 2 to 3 - Dir as Entry 5",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryFile(zaos, "test1/test2/test4.txt", "Install Test 4");
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/test2/test3/");
                    addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFileAtLevel2To3);
    }

    private static void pathAsserts_TwoDirsWithOneFileAtLevel2To3(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).doesNotExist();
        assertThat(destinationPath.resolve("test3")).isDirectory();
        assertThat(destinationPath.resolve("test3/test3.txt")).isRegularFile().hasContent("Install Test 3");
        assertThat(destinationPath.resolve("test4.txt")).isRegularFile().hasContent("Install Test 4");
    }

    static Arguments threeDirsWithOneFileAtLevel1To3() {
        return Arguments.of("Three Dirs with One File at Level 1 to 3", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
            addZipEntryFile(zaos, "test1/test2/test2.txt", "Install Test 2");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithOneFileAtLevel1To3);
    }

    static Arguments threeDirsWithOneFileAtLevel1To3_DirAsEntry() {
        return Arguments.of("Three Dirs with One File at Level 1 to 3 - Dir as Entry",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/test2/test3/");
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryFile(zaos, "test1/test2/test3/test3.txt", "Install Test 3");
                    addZipEntryFile(zaos, "test1/test2/test2.txt", "Install Test 2");
                    addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithOneFileAtLevel1To3);
    }

    private static void pathAsserts_ThreeDirsWithOneFileAtLevel1To3(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).isDirectory();
        assertThat(destinationPath.resolve("test2/test3")).isDirectory();
        assertThat(destinationPath.resolve("test1.txt")).isRegularFile().hasContent("Install Test 1");
        assertThat(destinationPath.resolve("test2/test2.txt")).isRegularFile().hasContent("Install Test 2");
        assertThat(destinationPath.resolve("test2/test3/test3.txt")).isRegularFile().hasContent("Install Test 3");
    }

    static Arguments threeDirsWithTwoFilesAtLevel2To3() {
        return Arguments.of("Three Dirs with Two Files at Level 2 to 3", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test4/test42.txt", "Install Test 42");
            addZipEntryFile(zaos, "test1/test2/test4/test41.txt", "Install Test 41");
            addZipEntryFile(zaos, "test1/test2/test3/test32.txt", "Install Test 32");
            addZipEntryFile(zaos, "test1/test2/test3/test31.txt", "Install Test 31");
            addZipEntryFile(zaos, "test1/test2/test22.txt", "Install Test 22");
            addZipEntryFile(zaos, "test1/test2/test21.txt", "Install Test 21");
        }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithTwoFilesAtLevel2To3);
    }

    static Arguments threeDirsWithTwoFilesAtLevel2To3_DirAsEntry() {
        return Arguments.of("Three Dirs with Two Files at Level 2 to 3 - Dir as Entry",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/test2/test4/");
                    addZipEntryDir(zaos, "test1/test2/test3/");
                    addZipEntryDir(zaos, "test1/test2/");
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryFile(zaos, "test1/test2/test4/test42.txt", "Install Test 42");
                    addZipEntryFile(zaos, "test1/test2/test4/test41.txt", "Install Test 41");
                    addZipEntryFile(zaos, "test1/test2/test3/test32.txt", "Install Test 32");
                    addZipEntryFile(zaos, "test1/test2/test3/test31.txt", "Install Test 31");
                    addZipEntryFile(zaos, "test1/test2/test22.txt", "Install Test 22");
                    addZipEntryFile(zaos, "test1/test2/test21.txt", "Install Test 21");
                }, (Consumer<Path>) ZipLevel3ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithTwoFilesAtLevel2To3);
    }

    private static void pathAsserts_ThreeDirsWithTwoFilesAtLevel2To3(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).doesNotExist();
        assertThat(destinationPath.resolve("test3")).isDirectory();
        assertThat(destinationPath.resolve("test4")).isDirectory();
        assertThat(destinationPath.resolve("test21.txt")).isRegularFile().hasContent("Install Test 21");
        assertThat(destinationPath.resolve("test22.txt")).isRegularFile().hasContent("Install Test 22");
        assertThat(destinationPath.resolve("test3/test31.txt")).isRegularFile().hasContent("Install Test 31");
        assertThat(destinationPath.resolve("test3/test32.txt")).isRegularFile().hasContent("Install Test 32");
        assertThat(destinationPath.resolve("test4/test41.txt")).isRegularFile().hasContent("Install Test 41");
        assertThat(destinationPath.resolve("test4/test42.txt")).isRegularFile().hasContent("Install Test 42");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zipTestCases")
    void testInstall(String testCase, Consumer<ZipArchiveOutputStream> zipEntries, Consumer<Path> assertions,
            WireMockRuntimeInfo wmRuntimeInfo) {
        testInstall(wmRuntimeInfo, zipEntries, assertions);
    }

}
