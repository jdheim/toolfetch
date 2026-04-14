/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.service.install.extract.scan.ArchiveScanner;
import com.jdheim.toolfetch.service.install.extract.scan.PathScanner;
import com.jdheim.toolfetch.service.install.extract.uncompress.CompositeArchiveUncompressor;
import com.jdheim.toolfetch.service.install.extract.uncompress.Uncompressor;
import com.jdheim.toolfetch.service.install.extract.validation.ArchiveZipBombValidator;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveExtractService implements ExtractService {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveExtractService.class);

    private static final String SLASH = "/";

    private static final int EXTRACT_BUFFER_SIZE = 8192;

    private final PathScanner pathScanner;

    private final Uncompressor uncompressor;

    public ArchiveExtractService() {
        pathScanner = new ArchiveScanner();
        uncompressor = new CompositeArchiveUncompressor();
    }

    @Override
    public void extract(Configuration configuration, Tool tool, Path archivePath) {
        Path destinationPath = archivePath.getParent();
        if (destinationPath == null) {
            LOG.warn("Destination Path could not be resolved. Skipping {}", tool.id());
            return;
        }
        LOG.info("Extract {} to {}", archivePath, destinationPath);
        try {
            extract(archivePath, destinationPath);
            LOG.info("Removing {}", archivePath);
            FileUtils.deleteQuietly(archivePath.toFile());
            if (FileUtils.isEmptyDirectory(destinationPath.toFile())) {
                LOG.warn("Nothing has been extracted. Removing {}", destinationPath);
                FileUtils.deleteQuietly(destinationPath.toFile());
            }
        } catch (Exception e) {
            LOG.warn("Extract failed due to exception: \"{}: {}\". Skipping {}", e.getClass().getName(), e.getMessage(),
                    tool.id());
            FileUtils.deleteQuietly(destinationPath.toFile());
        }
    }

    private void extract(Path archivePath, Path destinationPath) throws IOException {
        String topLevelDir = normalize(pathScanner.scan(archivePath));
        try (ArchiveWithCompressorInputStream acis = uncompressor.uncompress(archivePath)) {
            extractStream(acis, destinationPath, topLevelDir);
        }
    }

    private void extractStream(ArchiveWithCompressorInputStream acis, Path destinationPath, String topLevelDir) throws
            IOException {
        ArchiveEntry archiveEntry;
        int totalArchiveEntries = 0;
        long totalExtractedArchiveSize = 0L;

        while ((archiveEntry = acis.getNextEntry()) != null) {
            boolean canReadEntryData = acis.canReadEntryData(archiveEntry);
            if (!canReadEntryData || archiveEntry.isDirectory()) {
                if (!canReadEntryData) {
                    LOG.warn("Can't read archive entry at \"{}\". Skipping", archiveEntry);
                }
                continue;
            }

            ArchiveZipBombValidator.validateEntryCount(++totalArchiveEntries);

            String entryName = stripEntryName(archiveEntry, topLevelDir);
            Path target = resolveSecureTargetPath(destinationPath, entryName);

            totalExtractedArchiveSize = extractEntry(acis, archiveEntry, target, totalExtractedArchiveSize);

            Integer unixPermissions = readUnixPermissions(archiveEntry);
            if (unixPermissions != null) {
                applyUnixPermissions(target, unixPermissions);
            }
        }
    }

    String stripEntryName(ArchiveEntry archiveEntry, String topLevelDir) {
        String entryName = archiveEntry.getName();
        if (!topLevelDir.isEmpty()) {
            if (archiveEntry.isDirectory() && topLevelDir.startsWith(normalize(entryName))) {
                return StringUtils.EMPTY;
            }
            entryName = Strings.CS.removeStart(entryName, topLevelDir);
        }
        return entryName;
    }

    String normalize(String entryName) {
        if (StringUtils.isNotEmpty(entryName) && !entryName.endsWith(SLASH)) {
            entryName = entryName + SLASH;
        }
        return entryName;
    }

    /// See [Zip Slip vulnerability](https://security.snyk.io/research/zip-slip-vulnerability)
    private Path resolveSecureTargetPath(Path destinationPath, String entryName) {
        Path normalizedDestination = destinationPath.toAbsolutePath().normalize();
        Path normalizedTarget = normalizedDestination.resolve(entryName).normalize();
        if (!normalizedTarget.startsWith(normalizedDestination)) {
            throw new UnsupportedOperationException(
                    "Detected Zip Slip vulnerability: \"%s\" + \"%s\" = \"%s\"".formatted(normalizedDestination, entryName,
                            normalizedTarget));
        }
        return normalizedTarget;
    }

    long extractEntry(ArchiveWithCompressorInputStream acis, ArchiveEntry archiveEntry, Path target,
            long totalExtractedArchiveSize) throws IOException {
        Path parent = target.getParent();
        if (parent == null) {
            throw new UnsupportedOperationException(
                    "Detected Zip Slip vulnerability: Archive Entry \"%s\" does not have parent directory".formatted(target));
        }
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        byte[] extractBuffer = new byte[EXTRACT_BUFFER_SIZE];

        try (OutputStream out = Files.newOutputStream(target)) {
            int bytesRead;
            while ((bytesRead = acis.read(extractBuffer)) > 0) {
                ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry);
                totalExtractedArchiveSize += bytesRead;
                ArchiveZipBombValidator.validateTotalSize(totalExtractedArchiveSize);
                out.write(extractBuffer, 0, bytesRead);
            }
        }

        return totalExtractedArchiveSize;
    }

    private @Nullable Integer readUnixPermissions(ArchiveEntry archiveEntry) {
        if (archiveEntry instanceof TarArchiveEntry tarEntry) {
            return tarEntry.getMode();
        }
        if (archiveEntry instanceof ZipArchiveEntry zipEntry) {
            int mode = zipEntry.getUnixMode();
            return mode != 0 ? mode : null;
        }
        return null;
    }

    private void applyUnixPermissions(Path target, int unixPermissions) throws IOException {
        int perms = unixPermissions & 0770;

        try {
            Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(target);

            posixPermissions.remove(PosixFilePermission.OWNER_READ);
            posixPermissions.remove(PosixFilePermission.OWNER_WRITE);
            posixPermissions.remove(PosixFilePermission.OWNER_EXECUTE);
            posixPermissions.remove(PosixFilePermission.GROUP_READ);
            posixPermissions.remove(PosixFilePermission.GROUP_WRITE);
            posixPermissions.remove(PosixFilePermission.GROUP_EXECUTE);

            if ((perms & 0400) != 0) posixPermissions.add(PosixFilePermission.OWNER_READ);
            if ((perms & 0200) != 0) posixPermissions.add(PosixFilePermission.OWNER_WRITE);
            if ((perms & 0100) != 0) posixPermissions.add(PosixFilePermission.OWNER_EXECUTE);

            if ((perms & 0040) != 0) posixPermissions.add(PosixFilePermission.GROUP_READ);
            if ((perms & 0020) != 0) posixPermissions.add(PosixFilePermission.GROUP_WRITE);
            if ((perms & 0010) != 0) posixPermissions.add(PosixFilePermission.GROUP_EXECUTE);

            Files.setPosixFilePermissions(target, posixPermissions);
        } catch (UnsupportedOperationException _) {
            // Non-POSIX filesystem (e.g. Windows)
        }
    }

}
