/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.service.install.extract.uncompress.CompositeArchiveUncompressor;
import com.jdheim.toolfetch.service.install.extract.uncompress.Uncompressor;
import com.jdheim.toolfetch.service.install.extract.validation.ArchiveZipBombValidator;
import com.jdheim.toolfetch.service.log.LogHelper;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class ArchiveScanner implements PathScanner {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(ArchiveScanner.class);

    private static final int ZIP_SIGNATURE_SIZE = 4;

    private final Uncompressor uncompressor;

    public ArchiveScanner() {
        uncompressor = new CompositeArchiveUncompressor();
    }

    @Override
    public ScanResult scan(Path archivePath) throws IOException {
        LOGGER.log("archive-scan.started", archivePath);
        long startTime = System.nanoTime();
        try {
            try (InputStream in = Files.newInputStream(archivePath)) {
                byte[] signature = in.readNBytes(ZIP_SIGNATURE_SIZE);
                if (ZipArchiveInputStream.matches(signature, signature.length)) {
                    try (ZipFile zip = ZipFile.builder().setPath(archivePath).get()) {
                        return scanZip(zip);
                    }
                }
            }
            try (ArchiveWithCompressorInputStream acis = uncompressor.uncompress(archivePath)) {
                return scanStream(acis);
            }
        } finally {
            String elapsedTime = LogHelper.elapsedTime(startTime);
            LOGGER.log("archive-scan.completed", elapsedTime);
        }
    }

    private ScanResult scanZip(ZipFile zip) {
        Map<String, Integer> modes = new LinkedHashMap<>();
        Path topLevelDir = null;
        boolean commonParentPossible = true;
        int totalArchiveEntries = 0;
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory() || !zip.canReadEntryData(entry)) continue;
            int mode = entry.getUnixMode();
            if (entry.isUnixSymlink() || (mode & 0_110) != 0) {
                modes.put(entry.getName(), mode);
            }
            ArchiveZipBombValidator.validateEntryCount(++totalArchiveEntries);
            if (commonParentPossible) {
                topLevelDir = scanEntry(entry, topLevelDir);
                commonParentPossible = topLevelDir != null;
            }
        }
        return new ScanResult(topLevelDirPath(topLevelDir), Map.copyOf(modes));
    }

    private ScanResult scanStream(ArchiveWithCompressorInputStream acis) throws IOException {
        ArchiveEntry archiveEntry;
        Path topLevelDir = null;
        int totalArchiveEntries = 0;

        while ((archiveEntry = acis.getNextEntry()) != null) {
            if (!acis.canReadEntryData(archiveEntry) || archiveEntry.isDirectory()) continue;

            ArchiveZipBombValidator.validateEntryCount(++totalArchiveEntries);

            topLevelDir = scanEntry(archiveEntry, topLevelDir);
            if (topLevelDir == null) {
                return new ScanResult(StringUtils.EMPTY, Map.of());
            }
        }
        return new ScanResult(topLevelDirPath(topLevelDir), Map.of());
    }

    private String topLevelDirPath(@Nullable Path topLevelDir) {
        String topLevelDirPath = topLevelDir != null ? topLevelDir.toString() : StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(topLevelDirPath)) {
            LOGGER.log("archive-scan.top-level-dir-found", topLevelDirPath);
        }
        return topLevelDirPath;
    }

    private @Nullable Path scanEntry(ArchiveEntry archiveEntry, @Nullable Path topLevelDir) {
        Path archiveEntryParent = getParent(archiveEntry);
        if (archiveEntryParent == null) {
            return null;
        }
        if (topLevelDir == null) {
            topLevelDir = archiveEntryParent;
        } else if (!isWithinTopLevelDir(topLevelDir, archiveEntryParent)) {
            topLevelDir = getCommonParent(topLevelDir, archiveEntryParent);
        }
        return topLevelDir;
    }

    private @Nullable Path getParent(ArchiveEntry archiveEntry) {
        String entryName = archiveEntry.getName();
        return Path.of(entryName).getParent();
    }

    boolean isWithinTopLevelDir(Path topLevelDir, Path archiveEntryParent) {
        return topLevelDir.equals(archiveEntryParent) || archiveEntryParent.startsWith(topLevelDir);
    }

    @Nullable Path getCommonParent(Path topLevelDir, @Nullable Path archiveEntryParent) {
        if (archiveEntryParent == null) {
            return null;
        }
        if (isAncestorOfTopLevelDir(topLevelDir, archiveEntryParent)) {
            return archiveEntryParent;
        } else if (matchesFirstSegmentOfTopLevelDir(topLevelDir, archiveEntryParent)) {
            return getCommonParent(topLevelDir, archiveEntryParent.getParent());
        }
        return null;
    }

    boolean isAncestorOfTopLevelDir(Path topLevelDir, Path archiveEntryParent) {
        return topLevelDir.startsWith(archiveEntryParent);
    }

    boolean matchesFirstSegmentOfTopLevelDir(Path topLevelDir, Path archiveEntryParent) {
        return topLevelDir.getNameCount() > 0 && archiveEntryParent.getNameCount() > 0 && topLevelDir.getName(0)
                .equals(archiveEntryParent.getName(0));
    }

}
