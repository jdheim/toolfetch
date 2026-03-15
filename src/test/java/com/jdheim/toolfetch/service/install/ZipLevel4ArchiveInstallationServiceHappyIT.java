/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
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
class ZipLevel4ArchiveInstallationServiceHappyIT extends TestCommonArchiveInstallationService {

    static Stream<Arguments> zipTestCases() {
        return Stream.of(threeHundredDirsWithThreeFilesAtLevel2To4(), threeHundredDirsWithThreeFilesAtLevel2To4AndFileAtRoot(),
                threeDirsWithOneFile(), threeDirsWithOneFile_DirAsEntry());
    }

    static Arguments threeHundredDirsWithThreeFilesAtLevel2To4() {
        return Arguments.of("Three Hundred Dirs with Three Files at Level 2 to 4", (Consumer<ZipArchiveOutputStream>) zaos -> {
            for (int i = 1; i <= 100; i++) {
                addZipEntryFile(zaos, "test1/test2/test3/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test4/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test4/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test4/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
            }
        }, (Consumer<Path>) destinationPath -> {
            assertThat(destinationPath).isDirectory();
            assertThat(destinationPath.resolve("test1")).doesNotExist();
            assertThat(destinationPath.resolve("test2")).doesNotExist();
            assertThat(destinationPath.resolve("test3")).isDirectory();
            assertThat(destinationPath.resolve("test3/test4")).isDirectory();
            for (int i = 1; i <= 100; i++) {
                assertThat(destinationPath.resolve("test%d1.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d1".formatted(i));
                assertThat(destinationPath.resolve("test%d2.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d2".formatted(i));
                assertThat(destinationPath.resolve("test%d3.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d3".formatted(i));
                assertThat(destinationPath.resolve("test3/test%d1.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d1".formatted(i));
                assertThat(destinationPath.resolve("test3/test%d2.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d2".formatted(i));
                assertThat(destinationPath.resolve("test3/test%d3.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d3".formatted(i));
                assertThat(destinationPath.resolve("test3/test4/test%d1.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d1".formatted(i));
                assertThat(destinationPath.resolve("test3/test4/test%d2.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d2".formatted(i));
                assertThat(destinationPath.resolve("test3/test4/test%d3.txt".formatted(i))).exists()
                        .isRegularFile()
                        .hasContent("Install Test %d3".formatted(i));
            }
        });
    }

    static Arguments threeHundredDirsWithThreeFilesAtLevel2To4AndFileAtRoot() {
        return Arguments.of("Three Hundred Dirs with Three Files at Level 2 to 4 and File at Root",
                (Consumer<ZipArchiveOutputStream>) zaos -> {
                    for (int i = 1; i <= 100; i++) {
                        addZipEntryFile(zaos, "test1/test2/test3/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test3/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test3/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test3/test4/test%d3.txt".formatted(i),
                                "Install Test %d3".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test3/test4/test%d2.txt".formatted(i),
                                "Install Test %d2".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test3/test4/test%d1.txt".formatted(i),
                                "Install Test %d1".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                        addZipEntryFile(zaos, "test1/test2/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
                    }
                    addZipEntryFile(zaos, "test1.txt", "Install Test 1");
                }, (Consumer<Path>) destinationPath -> {
                    assertThat(destinationPath).isDirectory();
                    assertThat(destinationPath.resolve("test1")).isDirectory();
                    assertThat(destinationPath.resolve("test1/test2")).isDirectory();
                    assertThat(destinationPath.resolve("test1/test2/test3")).isDirectory();
                    assertThat(destinationPath.resolve("test1/test2/test3/test4")).isDirectory();
                    assertThat(destinationPath.resolve("test1.txt")).isRegularFile().hasContent("Install Test 1");
                    for (int i = 1; i <= 100; i++) {
                        assertThat(destinationPath.resolve("test1/test2/test%d1.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d1".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test%d2.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d2".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test%d3.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d3".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test3/test%d1.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d1".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test3/test%d2.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d2".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test3/test%d3.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d3".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test3/test4/test%d1.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d1".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test3/test4/test%d2.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d2".formatted(i));
                        assertThat(destinationPath.resolve("test1/test2/test3/test4/test%d3.txt".formatted(i))).exists()
                                .isRegularFile()
                                .hasContent("Install Test %d3".formatted(i));
                    }
                });
    }

    static Arguments threeDirsWithOneFile() {
        return Arguments.of("Three Dirs with One File", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryFile(zaos, "test1/test2/test3/test4/test4.txt", "Install Test 4");
            addZipEntryFile(zaos, "test5/test6/test7/test8/test8.txt", "Install Test 8");
            addZipEntryFile(zaos, "test9/test10/test11/test12/test12.txt", "Install Test 12");
        }, (Consumer<Path>) ZipLevel4ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithOneFile);
    }

    static Arguments threeDirsWithOneFile_DirAsEntry() {
        return Arguments.of("Three Dirs with One File - Dir as Entry", (Consumer<ZipArchiveOutputStream>) zaos -> {
            addZipEntryDir(zaos, "test9/");
            addZipEntryDir(zaos, "test9/test10/");
            addZipEntryDir(zaos, "test9/test10/test11/");
            addZipEntryDir(zaos, "test9/test10/test11/test12/");
            addZipEntryFile(zaos, "test9/test10/test11/test12/test12.txt", "Install Test 12");
            addZipEntryDir(zaos, "test5/");
            addZipEntryDir(zaos, "test5/test6/");
            addZipEntryDir(zaos, "test5/test6/test7/");
            addZipEntryDir(zaos, "test5/test6/test7/test8/");
            addZipEntryFile(zaos, "test5/test6/test7/test8/test8.txt", "Install Test 8");
            addZipEntryDir(zaos, "test1/");
            addZipEntryDir(zaos, "test1/test2/");
            addZipEntryDir(zaos, "test1/test2/test3/");
            addZipEntryDir(zaos, "test1/test2/test3/test4/");
            addZipEntryFile(zaos, "test1/test2/test3/test4/test4.txt", "Install Test 4");
        }, (Consumer<Path>) ZipLevel4ArchiveInstallationServiceHappyIT::pathAsserts_ThreeDirsWithOneFile);
    }

    private static void pathAsserts_ThreeDirsWithOneFile(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2/test3")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2/test3/test4")).isDirectory();
        assertThat(destinationPath.resolve("test1/test2/test3/test4/test4.txt")).exists()
                .isRegularFile()
                .hasContent("Install Test 4");
        assertThat(destinationPath.resolve("test5")).isDirectory();
        assertThat(destinationPath.resolve("test5/test6")).isDirectory();
        assertThat(destinationPath.resolve("test5/test6/test7")).isDirectory();
        assertThat(destinationPath.resolve("test5/test6/test7/test8")).isDirectory();
        assertThat(destinationPath.resolve("test5/test6/test7/test8/test8.txt")).exists()
                .isRegularFile()
                .hasContent("Install Test 8");
        assertThat(destinationPath.resolve("test9")).isDirectory();
        assertThat(destinationPath.resolve("test9/test10")).isDirectory();
        assertThat(destinationPath.resolve("test9/test10/test11")).isDirectory();
        assertThat(destinationPath.resolve("test9/test10/test11/test12")).isDirectory();
        assertThat(destinationPath.resolve("test9/test10/test11/test12/test12.txt")).exists()
                .isRegularFile()
                .hasContent("Install Test 12");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zipTestCases")
    void testInstall(String testCase, Consumer<ZipArchiveOutputStream> zipEntries, Consumer<Path> assertions,
            WireMockRuntimeInfo wmRuntimeInfo) {
        testInstall(wmRuntimeInfo, zipEntries, assertions);
    }

}
