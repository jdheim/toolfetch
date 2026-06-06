/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import com.jdheim.toolfetch.util.assertion.AssertionUtils;
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
    void testInstall_FilesAtRoot(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String archiveName = "sample1" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + archiveName, expectedCompressorName);

        testInstall(wmRuntimeInfo, archiveName, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample1Archive(destinationPath);
        });
    }

    @ParameterizedTest
    @CsvSource({
            ".tar,", ".tar.br,", ".tar.bz2, bzip2", "-deflate.tar, deflate", ".tar.gz, gz", "-concat.tar.bz2, bzip2",
            "-concat.tar.gz, gz", "-concat.tar.lz4, lz4-framed", "-concat.tar.xz, xz", ".tar.gz.gz.gz, gz", ".tar.gz.xz.gz, gz",
            ".tar.lz4, lz4-framed", ".tar.lzma, lzma", ".tar.sz, snappy-framed", ".tar.xz, xz", ".tar.zst, zstd", ".tar.Z, z"
    })
    void testInstall_Strip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample2" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample2Archive(destinationPath);
        });
    }

    @ParameterizedTest
    @CsvSource({
            ".tar,", ".tar.br,", ".tar.bz2, bzip2", "-deflate.tar, deflate", ".tar.gz, gz", "-concat.tar.bz2, bzip2",
            "-concat.tar.gz, gz", "-concat.tar.lz4, lz4-framed", "-concat.tar.xz, xz", ".tar.gz.gz.gz, gz", ".tar.gz.xz.gz, gz",
            ".tar.lz4, lz4-framed", ".tar.lzma, lzma", ".tar.sz, snappy-framed", ".tar.xz, xz", ".tar.zst, zstd", ".tar.Z, z"
    })
    void testInstall_FileAtRootNoStrip(String archiveSuffix, String expectedCompressorName, WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample3" + archiveSuffix;
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + filename, expectedCompressorName);

        testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertNoErrorNoWarn();
            AssertionUtils.assertSample3Archive(destinationPath);
        });
    }

    @Test
    void testInstall_BrotliNotAvailable(WireMockRuntimeInfo wmRuntimeInfo) {
        String filename = "sample1.tar.br";
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/tar/" + filename);

        try (MockedStatic<BrotliUtils> brotliUtils = mockStatic()) {
            brotliUtils.when(BrotliUtils::isBrotliCompressionAvailable).thenReturn(false);

            testInstall(wmRuntimeInfo, filename, archiveBytes, destinationPath -> {
                getTestLogListAppender().assertAnyMatch(Level.WARN,
                        "Extract failed due to exception: \"org.apache.commons.compress.compressors.CompressorException: "
                                + "Brotli compression is not available. In addition to Apache Commons Compress you need the Google Brotli Dec library - see https://github.com/google/brotli/\". Skipping toolfetch");
                assertThat(destinationPath).doesNotExist();
            });
        }
    }

}
