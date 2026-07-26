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
class ZipLevel1ArchiveInstallationServiceHappyIT extends TestCommonArchiveInstallationService {

    static Stream<Arguments> zipTestCases() {
        return Stream.of(oneFile(), oneDirWithOneFile(), oneDirWithOneFile_DirAsEntry(), oneDirWithOneFile_ZipSlip(),
                oneDirWithOneFile_DirAsEntry_ZipSlip(), oneDirWithTwoFiles(), oneDirWithTwoFiles_DirAsEntry(),
                oneDirWithOneFileAndFileAtRoot(), oneDirWithOneFileAndFileAtRoot_DirAsEntry(), twoDirsWithOneFile(),
                twoDirsWithOneFile_DirAsEntry(), threeDirsWithOneFile(), threeDirsWithOneFile_DirAsEntry(),
                twoDirsWithTwoFilesAndFilesAtRoot(), twoDirsWithTwoFilesAndFilesAtRoot_DirAsEntry());
    }

    static Arguments oneFile() {
        return Arguments.of("One File",
                (Consumer<ZipArchiveOutputStream>) zaos -> addZipEntryFile(zaos, "test1.txt", "Install Test 1"),
                (Consumer<Path>) destinationPath -> {
                    assertThat(destinationPath).isDirectory();
                    assertThat(destinationPath.resolve("test1.txt")).isRegularFile().hasContent("Install Test 1");
                });
    }

    static Arguments oneDirWithOneFile() {
        return Arguments.of("One Dir with One File",
                (Consumer<ZipArchiveOutputStream>) zaos -> addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1"),
                (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile);
    }

    static Arguments oneDirWithOneFile_DirAsEntry() {
        return Arguments.of("One Dir with One File - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile);
    }

    static Arguments oneDirWithOneFile_ZipSlip() {
        return Arguments.of("One Dir with One File - Zip Slip",
                (Consumer<ZipArchiveOutputStream>) zaos -> addZipEntryFile(zaos, "../test1.txt", "Install Test 1"),
                (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile);
    }

    static Arguments oneDirWithOneFile_DirAsEntry_ZipSlip() {
        return Arguments.of("One Dir with One File - Dir as Entry - ZipSlip", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "../");
            addZipEntryFile(zaos, "../test1.txt", "Install Test 1");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFile);
    }

    private static void pathAsserts_OneDirWithOneFile(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test1.txt")).isRegularFile().hasContent("Install Test 1");
    }

    static Arguments oneDirWithTwoFiles() {
        return Arguments.of("One Dir with Two Files", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
            addZipEntryFile(zaos, "test1/test2.txt", "Install Test 2");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithTwoFiles);
    }

    static Arguments oneDirWithTwoFiles_DirAsEntry() {
        return Arguments.of("One Dir with Two Files - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
            addZipEntryFile(zaos, "test1/test2.txt", "Install Test 2");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithTwoFiles);
    }

    private static void pathAsserts_OneDirWithTwoFiles(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test1.txt")).isRegularFile().hasContent("Install Test 1");
        assertThat(destinationPath.resolve("test2.txt")).isRegularFile().hasContent("Install Test 2");
    }

    static Arguments oneDirWithOneFileAndFileAtRoot() {
        return Arguments.of("One Dir with One File and File at Root", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
            addZipEntryFile(zaos, "test2.txt", "Install Test 2");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFileAndFileAtRoot);
    }

    static Arguments oneDirWithOneFileAndFileAtRoot_DirAsEntry() {
        return Arguments.of("One Dir with One File and File at Root - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test2.txt", "Install Test 2");
            addZipEntryDir(zaos, "test1/");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_OneDirWithOneFileAndFileAtRoot);
    }

    private static void pathAsserts_OneDirWithOneFileAndFileAtRoot(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test1.txt")).isRegularFile().hasContent("Install Test 1");
        assertThat(destinationPath.resolve("test2.txt")).isRegularFile().hasContent("Install Test 2");
    }

    static Arguments twoDirsWithOneFile() {
        return Arguments.of("Two Dirs with One File", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test2/test2.txt", "Install Test 2");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFile);
    }

    static Arguments twoDirsWithOneFile_DirAsEntry() {
        return Arguments.of("Two Dirs with One File - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test1/");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
            addZipEntryDir(zaos, "test2/");
            addZipEntryFile(zaos, "test2/test2.txt", "Install Test 2");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithOneFile);
    }

    private static void pathAsserts_TwoDirsWithOneFile(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test1.txt")).isRegularFile().hasContent("Install Test 1");
        assertThat(destinationPath.resolve("test2")).isDirectory();
        assertThat(destinationPath.resolve("test2/test2.txt")).isRegularFile().hasContent("Install Test 2");
    }

    static Arguments threeDirsWithOneFile() {
        return Arguments.of("Three Dirs with One File", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
            addZipEntryFile(zaos, "test2/test2.txt", "Install Test 2");
            addZipEntryFile(zaos, "test3/test3.txt", "Install Test 3");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithOneFile);
    }

    static Arguments threeDirsWithOneFile_DirAsEntry() {
        return Arguments.of("Three Dirs with One File - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test3/");
            addZipEntryFile(zaos, "test3/test3.txt", "Install Test 3");
            addZipEntryDir(zaos, "test2/");
            addZipEntryFile(zaos, "test2/test2.txt", "Install Test 2");
            addZipEntryDir(zaos, "test1/");
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithOneFile);
    }

    private static void pathAsserts_ThreeDirsWithOneFile(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test1.txt")).isRegularFile().hasContent("Install Test 1");
        assertThat(destinationPath.resolve("test2")).isDirectory();
        assertThat(destinationPath.resolve("test2/test2.txt")).isRegularFile().hasContent("Install Test 2");
        assertThat(destinationPath.resolve("test3")).isDirectory();
        assertThat(destinationPath.resolve("test3/test3.txt")).isRegularFile().hasContent("Install Test 3");
    }

    static Arguments twoDirsWithTwoFilesAndFilesAtRoot() {
        return Arguments.of("Two Dirs with Two Files and Files at Root", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test3/test32.txt", "Install Test 32");
            addZipEntryFile(zaos, "test3/test31.txt", "Install Test 31");
            addZipEntryFile(zaos, "test2/test22.txt", "Install Test 22");
            addZipEntryFile(zaos, "test2/test21.txt", "Install Test 21");
            addZipEntryFile(zaos, "test12.txt", "Install Test 12");
            addZipEntryFile(zaos, "test11.txt", "Install Test 11");
        }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithTwoFilesAndFilesAtRoot);
    }

    static Arguments twoDirsWithTwoFilesAndFilesAtRoot_DirAsEntry() {
        return Arguments.of("Two Dirs with Two Files and Files at Root - Dir as Entry",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test3/");
                    addZipEntryDir(zaos, "test2/");
                    addZipEntryFile(zaos, "test3/test32.txt", "Install Test 32");
                    addZipEntryFile(zaos, "test3/test31.txt", "Install Test 31");
                    addZipEntryFile(zaos, "test2/test22.txt", "Install Test 22");
                    addZipEntryFile(zaos, "test2/test21.txt", "Install Test 21");
                    addZipEntryFile(zaos, "test12.txt", "Install Test 12");
                    addZipEntryFile(zaos, "test11.txt", "Install Test 11");
                }, (Consumer<Path>) ZipLevel1ArchiveInstallationServiceHappyIT::pathAsserts_TwoDirsWithTwoFilesAndFilesAtRoot);
    }

    private static void pathAsserts_TwoDirsWithTwoFilesAndFilesAtRoot(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
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
