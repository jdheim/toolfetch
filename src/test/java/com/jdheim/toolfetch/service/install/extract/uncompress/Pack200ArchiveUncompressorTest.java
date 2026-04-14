/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// OOC Tests for [Pack200ArchiveUncompressor]
class Pack200ArchiveUncompressorTest {

    Pack200ArchiveUncompressor uncompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uncompressor = new Pack200ArchiveUncompressor();
    }

    @ParameterizedTest
    @ValueSource(strings = {"toolfetch.gz", "toolfetch.pack.gz"})
    void testUncompress(String archiveName) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(InputStream.nullInputStream());
        BufferedInputStream bufferedCis = uncompressor.createCompressorInputStream(bis, tempDir.resolve(archiveName)).getLeft();
        assertThat(bufferedCis).isEqualTo(bis);
    }

}
