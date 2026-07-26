/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import java.io.IOException;
import java.nio.file.Path;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.service.install.extract.uncompress.CompositeArchiveUncompressor;
import com.jdheim.toolfetch.service.install.extract.uncompress.Uncompressor;
import com.jdheim.toolfetch.service.install.extract.validation.ArchiveZipBombValidator;
import com.jdheim.toolfetch.service.log.LogHelper;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveScanner implements PathScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveScanner.class);

    private final Uncompressor uncompressor;

    public ArchiveScanner() {
        uncompressor = new CompositeArchiveUncompressor();
    }

    @Override
    public String scan(Path archivePath) throws IOException {
        LOG.info("Scanning {}", archivePath);
        long startTime = System.nanoTime();
        try (ArchiveWithCompressorInputStream acis = uncompressor.uncompress(archivePath)) {
            return scanStream(acis);
        } finally {
            String elapsedTime = LogHelper.elapsedTime(startTime);
            LOG.info("Scan completed in {}s", elapsedTime);
        }
    }

    private String scanStream(ArchiveWithCompressorInputStream acis) throws IOException {
        ArchiveEntry archiveEntry;
        Path topLevelDir = null;
        int totalArchiveEntries = 0;

        while ((archiveEntry = acis.getNextEntry()) != null) {
            if (!acis.canReadEntryData(archiveEntry) || archiveEntry.isDirectory()) continue;

            ArchiveZipBombValidator.validateEntryCount(++totalArchiveEntries);

            topLevelDir = scanEntry(archiveEntry, topLevelDir);
            if (topLevelDir == null) {
                return StringUtils.EMPTY;
            }
        }
        String topLevelDirPath = topLevelDir != null ? topLevelDir.toString() : StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(topLevelDirPath)) {
            LOG.info("Top-level directory \"{}\" detected. Stripping during extraction", topLevelDirPath);
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
        return topLevelDir.getNameCount() > 0 && archiveEntryParent.getNameCount() > 0 &&
                topLevelDir.getName(0).equals(archiveEntryParent.getName(0));
    }

}
