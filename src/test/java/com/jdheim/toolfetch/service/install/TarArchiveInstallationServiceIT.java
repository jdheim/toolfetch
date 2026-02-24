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
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import org.apache.commons.compress.compressors.brotli.BrotliUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/// Integration Tests for [ArchiveInstallationService]
/// Commands to create:
/// - Deflate .tar: `tar -cf - test1.txt test2.txt test3.txt | openssl zlib -e > sample1-deflate.tar`
/// - Concat .tar.bz2: `split -b 1k sample1.tar part_ && for f in part_*; do bzip2 -c "$f" > "$f.bz2"; done && cat part_*.bz2 > sample1-concat.tar.bz2`
/// - Concat .tar.gz: `split -b 1k sample1.tar part_ && for f in part_*; do gzip -c "$f" > "$f.gz"; done && cat part_*.gz > sample1-concat.tar.gz`
/// - Concat .tar.lz4: `split -b 1k sample1.tar part_ && for f in part_*; do lz4 -c "$f" > "$f.lz4"; done && cat part_*.lz4 > sample1-concat.tar.lz4`
/// - Concat .tar.xz: `split -b 1k sample1.tar part_ && for f in part_*; do xz -c "$f" > "$f.xz"; done && cat part_*.xz > sample1-concat.tar.xz`
@WireMockTest
class TarArchiveInstallationServiceIT extends TestCommonArchiveInstallationService {

    @ParameterizedTest
    @CsvSource({
            ".tar,", ".tar.br,", ".tar.bz2, bzip2", "-deflate.tar, deflate", ".tar.gz, gz", "-concat.tar.bz2, bzip2",
            "-concat.tar.gz, gz", "-concat.tar.lz4, lz4-framed", "-concat.tar.xz, xz", ".tar.gz.gz.gz, gz", ".tar.gz.xz.gz, gz",
            ".tar.lz4, lz4-framed", ".tar.lzma, lzma", ".tar.sz, snappy-framed", ".tar.xz, xz", ".tar.zst, zstd", ".tar.Z, z"
    })
    void testInstall_FilesAtRoot(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) throws
            IOException {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            assertThat(destinationPath).isDirectory();
            for (int i = 1; i <= 3; i++) {
                assertThat(destinationPath.resolve("test%d.txt".formatted(i))).isRegularFile()
                        .hasContent("Hello ToolFetch %d".formatted(i));
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
            ".tar,", ".tar.br,", ".tar.bz2, bzip2", "-deflate.tar, deflate", ".tar.gz, gz", "-concat.tar.bz2, bzip2",
            "-concat.tar.gz, gz", "-concat.tar.lz4, lz4-framed", "-concat.tar.xz, xz", ".tar.gz.gz.gz, gz", ".tar.gz.xz.gz, gz",
            ".tar.lz4, lz4-framed", ".tar.lzma, lzma", ".tar.sz, snappy-framed", ".tar.xz, xz", ".tar.zst, zstd", ".tar.Z, z"
    })
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) throws
            IOException {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + filename, expectedCompressorName);

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
    @CsvSource({
            ".tar,", ".tar.br,", ".tar.bz2, bzip2", "-deflate.tar, deflate", ".tar.gz, gz", "-concat.tar.bz2, bzip2",
            "-concat.tar.gz, gz", "-concat.tar.lz4, lz4-framed", "-concat.tar.xz, xz", ".tar.gz.gz.gz, gz", ".tar.gz.xz.gz, gz",
            ".tar.lz4, lz4-framed", ".tar.lzma, lzma", ".tar.sz, snappy-framed", ".tar.xz, xz", ".tar.zst, zstd", ".tar.Z, z"
    })
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName,
            WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + filename, expectedCompressorName);

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

    @Test
    void testInstall_BrotliNotAvailable(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String filename = "sample1.tar.br";
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + filename);

        try (MockedStatic<BrotliUtils> brotliUtils = mockStatic()) {
            brotliUtils.when(BrotliUtils::isBrotliCompressionAvailable).thenReturn(false);

            testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
                getTestLogListAppender().assertAnyMatch(Level.WARN,
                        "Extract failed due to exception: \"No Archiver found for the stream signature\". Skipping toolfetch");
                assertThat(destinationPath).doesNotExist();
            });
        }
    }

}
