/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.util.archive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.brotli.BrotliUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public final class ArchiveUtils {

    private static final String BROTLI_SUFFIX = ".br";

    private ArchiveUtils() {
        throw new AssertionError();
    }

    public static byte[] readTestFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            assertThat(in).withFailMessage("Test File not found: " + path).isNotNull();
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] readTestFile(String path) {
        return readTestFile(path, null);
    }

    public static byte[] readTestFile(String path, @Nullable String expectedCompressorName) {
        try (InputStream in = ArchiveUtils.class.getResourceAsStream(path)) {
            assertThat(in).withFailMessage("Test File not found: " + path).isNotNull();
            byte[] data = in.readAllBytes();
            assertCompressorName(path, expectedCompressorName, data);
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void assertCompressorName(String path, @Nullable String expectedCompressorName, byte[] data) throws
            CompressorException {
        try {
            String compressorName = CompressorStreamFactory.detect(new ByteArrayInputStream(data));
            assertThat(compressorName).isEqualTo(expectedCompressorName);
        } catch (CompressorException e) {
            if (path.toLowerCase(Locale.ROOT).endsWith(BROTLI_SUFFIX)) {
                assertThat(BrotliUtils.isBrotliCompressionAvailable()).isTrue();
            } else if (StringUtils.isNotEmpty(expectedCompressorName)) {
                throw e;
            }
        }
    }

    public static byte[] createZip(Consumer<ZipArchiveOutputStream> zipEntries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(baos)) {
            zipEntries.accept(zaos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    public static void addZipEntryFile(ZipArchiveOutputStream zaos, String path, String content) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        ZipArchiveEntry archiveEntry = new ZipArchiveEntry(path);
        archiveEntry.setSize(data.length);
        try {
            zaos.putArchiveEntry(archiveEntry);
            zaos.write(data);
            zaos.closeArchiveEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void addZipEntryDir(ZipArchiveOutputStream zaos, String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        ZipArchiveEntry archiveEntry = new ZipArchiveEntry(path);
        try {
            zaos.putArchiveEntry(archiveEntry);
            zaos.closeArchiveEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
