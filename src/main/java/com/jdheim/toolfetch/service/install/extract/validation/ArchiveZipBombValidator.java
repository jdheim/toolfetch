/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.validation;

import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.io.FileUtils;

public final class ArchiveZipBombValidator {

    private static final int THRESHOLD_ENTRIES = 10_000;

    private static final long MIN_COMPRESSED_SIZE = 1024L * 1024; // 1 MB

    private static final long THRESHOLD_SIZE = 10L * 1024 * 1024 * 1024; // 10 GB

    private static final double THRESHOLD_RATIO = 100.0;

    ArchiveZipBombValidator() {
        throw new AssertionError();
    }

    public static void validateEntryCount(int totalArchiveEntries) {
        if (totalArchiveEntries > THRESHOLD_ENTRIES) {
            throw new UnsupportedOperationException(
                    "Detected Zip Bomb vulnerability: Archive contains more than %s entries".formatted(THRESHOLD_ENTRIES));
        }
    }

    public static void validateTotalSize(long totalExtractedArchiveSize) {
        if (totalExtractedArchiveSize > THRESHOLD_SIZE) {
            throw new UnsupportedOperationException(
                    "Detected Zip Bomb vulnerability: Archive is larger than %s when extracted".formatted(
                            FileUtils.byteCountToDisplaySize(THRESHOLD_SIZE)));
        }
    }

    public static void validateCompressionRatio(ArchiveWithCompressorInputStream acis, ArchiveEntry archiveEntry) {
        if (acis.hasInputStreamStatistics()) {
            long compressedSize = acis.getCompressedCount();
            if (compressedSize > MIN_COMPRESSED_SIZE) {
                long extractedSize = acis.getUncompressedCount();
                double compressionRatio = (double) extractedSize / compressedSize;
                if (compressionRatio > THRESHOLD_RATIO) {
                    throw new UnsupportedOperationException(
                            "Detected Zip Bomb vulnerability: Archive Entry \"%s\" has suspicious compression ratio \"%.2f\" (compressed size = %s, extracted size = %s)".formatted(
                                    archiveEntry, compressionRatio, FileUtils.byteCountToDisplaySize(compressedSize),
                                    FileUtils.byteCountToDisplaySize(extractedSize)));
                }
            }
        }
    }

}
