/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemLoopException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.exception.SkippedArchiveException;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.service.install.extract.scan.ArchiveScanner;
import com.jdheim.toolfetch.service.install.extract.scan.PathScanner;
import com.jdheim.toolfetch.service.install.extract.scan.ScanResult;
import com.jdheim.toolfetch.service.install.extract.uncompress.CompositeArchiveUncompressor;
import com.jdheim.toolfetch.service.install.extract.uncompress.Uncompressor;
import com.jdheim.toolfetch.service.install.extract.validation.ArchiveZipBombValidator;
import com.jdheim.toolfetch.service.log.LogHelper;
import com.jdheim.toolfetch.service.util.CharacterConstants;
import com.jdheim.toolfetch.service.util.PathHelper;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipShort;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;

public class ArchiveExtractService implements ExtractService {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(ArchiveExtractService.class);

    private static final int EXTRACT_BUFFER_SIZE = 8192;

    private static final int MAX_LINK_TARGET_SIZE = 4096;

    private static final int MAX_SYMBOLIC_LINK_DEPTH = 40;

    private static final ZipShort PKWARE_UNIX_EXTRA_FIELD_ID = new ZipShort(0x000d);

    private static final int PKWARE_UNIX_TARGET_OFFSET = 12;

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
            LOGGER.log("archive-extract.destination-not-found", tool.id());
            return;
        }
        try {
            extract(archivePath, destinationPath);
            cleanup(archivePath, destinationPath);
        } catch (SkippedArchiveException e) {
            LOGGER.log(e);
            LOGGER.log("archive-extract.binary-file-found");
            tryRenameAndSetExecutable(archivePath, tool);
        } catch (Exception e) {
            LOGGER.log(e, "archive-extract.exception", tool.id());
            PathHelper.deleteQuietly(destinationPath);
        }
    }

    private void extract(Path archivePath, Path destinationPath) throws IOException {
        ScanResult scanResult = pathScanner.scan(archivePath);
        LOGGER.log("archive-extract.started", archivePath, destinationPath);
        long startTime = System.nanoTime();
        try (ArchiveWithCompressorInputStream acis = uncompressor.uncompress(archivePath)) {
            extractStream(acis, destinationPath, scanResult);
        } finally {
            String elapsedTime = LogHelper.elapsedTime(startTime);
            LOGGER.log("archive-extract.completed", elapsedTime);
        }
    }

    private void extractStream(ArchiveWithCompressorInputStream acis, Path destinationPath, ScanResult scanResult) throws
            IOException {
        ArchiveEntry archiveEntry;
        int totalArchiveEntries = 0;
        long totalExtractedArchiveSize = 0L;
        Map<Path, Path> symbolicLinks = new LinkedHashMap<>();
        Map<Path, Path> hardLinks = new LinkedHashMap<>();

        while ((archiveEntry = acis.getNextEntry()) != null) {
            boolean canReadEntryData = acis.canReadEntryData(archiveEntry);
            if (!canReadEntryData || archiveEntry.isDirectory()) {
                if (!canReadEntryData) {
                    LOGGER.log("archive-extract.entry-failed", archiveEntry);
                }
                continue;
            }

            ArchiveZipBombValidator.validateEntryCount(++totalArchiveEntries);

            String entryName = stripEntryName(archiveEntry, scanResult.topLevelDir());
            Path target = resolveSecureTargetPath(destinationPath, entryName);
            validateExistingTargetPath(destinationPath, target);

            symbolicLinks.remove(target);
            hardLinks.remove(target);
            totalExtractedArchiveSize = extractArchiveEntry(acis, archiveEntry, target, scanResult, symbolicLinks, hardLinks,
                    totalExtractedArchiveSize);
            createHardLinks(destinationPath, hardLinks, symbolicLinks, false);
        }
        createHardLinks(destinationPath, hardLinks, symbolicLinks, true);
        createSymbolicLinks(destinationPath, symbolicLinks);
    }

    private long extractArchiveEntry(ArchiveWithCompressorInputStream acis, ArchiveEntry archiveEntry, Path target,
            ScanResult scanResult, Map<Path, Path> symbolicLinks, Map<Path, Path> hardLinks,
            long totalExtractedArchiveSize) throws IOException {
        if (archiveEntry instanceof TarArchiveEntry tarEntry && (tarEntry.isSymbolicLink() || tarEntry.isLink())) {
            String linkTarget = tarEntry.getLinkName();
            validateLinkTargetSize(linkTarget.getBytes(StandardCharsets.UTF_8).length, archiveEntry, scanResult);
            if (tarEntry.isLink()) {
                hardLinks.put(target, stripHardLinkTarget(linkTarget, scanResult.topLevelDir()));
            } else {
                symbolicLinks.put(target, Path.of(linkTarget));
            }
            byte[] payload = readLinkPayload(acis, archiveEntry, scanResult, totalExtractedArchiveSize);
            return totalExtractedArchiveSize + payload.length;
        }

        if (archiveEntry instanceof ZipArchiveEntry zipEntry) {
            zipEntry.setUnixMode(scanResult.zipMode(zipEntry.getDataOffset()));
            ZipExtraField unixField = zipEntry.getExtraField(PKWARE_UNIX_EXTRA_FIELD_ID);
            if (unixField != null && (zipEntry.isUnixSymlink()
                    || (zipEntry.getUnixMode() & UnixStat.FILE_TYPE_FLAG) == UnixStat.FILE_FLAG)) {
                byte[] unixData = unixField.getLocalFileDataData();
                if (unixData.length > PKWARE_UNIX_TARGET_OFFSET) {
                    int targetSize = unixData.length - PKWARE_UNIX_TARGET_OFFSET;
                    validateLinkTargetSize(targetSize, archiveEntry, scanResult);
                    String linkTarget = StandardCharsets.UTF_8.newDecoder()
                            .decode(ByteBuffer.wrap(unixData, PKWARE_UNIX_TARGET_OFFSET, targetSize))
                            .toString();
                    if (zipEntry.isUnixSymlink()) {
                        symbolicLinks.put(target, Path.of(linkTarget));
                    } else {
                        hardLinks.put(target, stripHardLinkTarget(linkTarget, scanResult.topLevelDir()));
                    }
                    byte[] payload = readLinkPayload(acis, archiveEntry, scanResult, totalExtractedArchiveSize);
                    return totalExtractedArchiveSize + payload.length;
                }
            }
            if (zipEntry.isUnixSymlink()) {
                byte[] linkTarget = readLinkPayload(acis, archiveEntry, scanResult, totalExtractedArchiveSize);
                validateLinkTargetSize(linkTarget.length, archiveEntry, scanResult);
                symbolicLinks.put(target,
                        Path.of(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(linkTarget)).toString()));
                return totalExtractedArchiveSize + linkTarget.length;
            }
        }

        long extractedSize = extractEntry(acis, archiveEntry, target, totalExtractedArchiveSize);
        Integer unixPermissions = readUnixPermissions(archiveEntry);
        if (unixPermissions != null) {
            applyUnixPermissions(target, unixPermissions);
        }
        return extractedSize;
    }

    private byte[] readLinkPayload(ArchiveWithCompressorInputStream acis, ArchiveEntry archiveEntry,
            ScanResult scanResult, long totalExtractedArchiveSize) throws IOException {
        ByteArrayOutputStream linkTarget = new ByteArrayOutputStream();
        byte[] buffer = new byte[MAX_LINK_TARGET_SIZE + 1];
        int bytesRead;
        while ((bytesRead = acis.read(buffer)) > 0) {
            int linkTargetSize = linkTarget.size() + bytesRead;
            ArchiveZipBombValidator.validateCompressionRatio(acis, archiveEntry);
            ArchiveZipBombValidator.validateTotalSize(totalExtractedArchiveSize + linkTargetSize);
            validateLinkTargetSize(linkTargetSize, archiveEntry, scanResult);
            linkTarget.write(buffer, 0, bytesRead);
        }
        return linkTarget.toByteArray();
    }

    private void validateLinkTargetSize(int size, ArchiveEntry archiveEntry, ScanResult scanResult) {
        if (size == 0) {
            throw new UnsupportedOperationException(ToolFetchLogger.message("exception.archive-extract.link-target-empty",
                    stripEntryName(archiveEntry, scanResult.topLevelDir())));
        }
        if (size > MAX_LINK_TARGET_SIZE) {
            throw new UnsupportedOperationException(ToolFetchLogger.message("exception.archive-extract.link-target-too-large",
                    stripEntryName(archiveEntry, scanResult.topLevelDir()), MAX_LINK_TARGET_SIZE));
        }
    }

    private Path stripHardLinkTarget(String linkTarget, String topLevelDir) {
        Path normalizedTarget = Path.of(linkTarget).normalize();
        if (!topLevelDir.isEmpty()) {
            Path normalizedRoot = Path.of(topLevelDir).normalize();
            if (normalizedTarget.startsWith(normalizedRoot)) {
                return normalizedRoot.relativize(normalizedTarget);
            }
        }
        return normalizedTarget;
    }

    private void createHardLinks(Path destinationPath, Map<Path, Path> hardLinks, Map<Path, Path> symbolicLinks,
            boolean required) throws IOException {
        while (!hardLinks.isEmpty()) {
            boolean createdLink = false;
            Iterator<Map.Entry<Path, Path>> links = hardLinks.entrySet().iterator();
            while (links.hasNext()) {
                Map.Entry<Path, Path> link = links.next();
                Path linkTarget = resolveSecureTargetPath(destinationPath, link.getValue().toString());
                if (hardLinks.containsKey(linkTarget)) {
                    continue;
                }
                if (symbolicLinks.containsKey(linkTarget) || Files.isSymbolicLink(linkTarget)) {
                    validateExistingTargetPath(destinationPath, linkTarget);
                    throw new IOException(
                            ToolFetchLogger.message("exception.archive-extract.hard-link-target-not-file", linkTarget));
                }
                if (!required && !Files.exists(linkTarget, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                resolveSecureTargetPath(destinationPath.toRealPath(), linkTarget.toRealPath().toString());
                if (!Files.isRegularFile(linkTarget, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException(
                            ToolFetchLogger.message("exception.archive-extract.hard-link-target-not-file", linkTarget));
                }
                Files.createDirectories(requireParent(link.getKey()));
                deleteRegularFile(link.getKey());
                Files.createLink(link.getKey(), linkTarget);
                links.remove();
                createdLink = true;
            }
            if (!createdLink) {
                if (required) {
                    throw new IOException(ToolFetchLogger.message("exception.archive-extract.hard-link-cycle", hardLinks));
                }
                return;
            }
        }
    }

    private void createSymbolicLinks(Path destinationPath, Map<Path, Path> symbolicLinks) throws IOException {
        for (Path link : symbolicLinks.keySet()) {
            Files.createDirectories(requireParent(link));
        }
        for (Map.Entry<Path, Path> link : symbolicLinks.entrySet()) {
            Path parent = requireParent(link.getKey());
            Path linkTarget = link.getValue();
            resolveSecureTargetPath(destinationPath, parent.resolve(linkTarget).toString());
            deleteRegularFile(link.getKey());
            Files.createSymbolicLink(link.getKey(), linkTarget);
        }
        for (Path link : symbolicLinks.keySet()) {
            validateExistingTargetPath(destinationPath, link);
        }
    }

    /// Checks existing ancestors as well as the lexical path, including for targets that do not exist yet.
    private void validateExistingTargetPath(Path destinationPath, Path target) throws IOException {
        Path resolved = resolveExistingPath(target, MAX_SYMBOLIC_LINK_DEPTH);
        resolveSecureTargetPath(destinationPath.toRealPath(), resolved.toString());
    }

    private Path resolveExistingPath(Path target, int remainingLinks) throws IOException {
        Path absoluteTarget = target.toAbsolutePath();
        Path resolved = Objects.requireNonNull(absoluteTarget.getRoot());
        Deque<Path> components = new ArrayDeque<>();
        absoluteTarget.forEach(components::addLast);
        while (!components.isEmpty()) {
            resolved = resolved.resolve(components.removeFirst()).normalize();
            if (Files.isSymbolicLink(resolved)) {
                if (remainingLinks-- == 0) {
                    throw new FileSystemLoopException(resolved.toString());
                }
                Path linkTarget = Files.readSymbolicLink(resolved);
                resolved = linkTarget.isAbsolute() ? Objects.requireNonNull(linkTarget.getRoot()) : requireParent(resolved);
                for (int i = linkTarget.getNameCount() - 1; i >= 0; i--) {
                    components.addFirst(linkTarget.getName(i));
                }
            } else if (Files.exists(resolved, LinkOption.NOFOLLOW_LINKS)) {
                resolved = resolved.toRealPath();
            }
        }
        return resolved;
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
        if (StringUtils.isNotEmpty(entryName) && !entryName.endsWith(CharacterConstants.SLASH)) {
            entryName += CharacterConstants.SLASH;
        }
        return entryName;
    }

    /// See [Zip Slip vulnerability](https://security.snyk.io/research/zip-slip-vulnerability)
    private Path resolveSecureTargetPath(Path destinationPath, String entryName) {
        Path normalizedDestination = destinationPath.toAbsolutePath().normalize();
        Path normalizedTarget = normalizedDestination.resolve(entryName).normalize();
        if (!normalizedTarget.startsWith(normalizedDestination)) {
            throw new UnsupportedOperationException(
                    ToolFetchLogger.message("exception.archive-extract.zip-slip", normalizedDestination, entryName,
                            normalizedTarget));
        }
        return normalizedTarget;
    }

    long extractEntry(ArchiveWithCompressorInputStream acis, ArchiveEntry archiveEntry, Path target,
            long totalExtractedArchiveSize) throws IOException {
        Path parent = requireParent(target);
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        byte[] extractBuffer = new byte[EXTRACT_BUFFER_SIZE];

        deleteRegularFile(target);
        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
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

    private void deleteRegularFile(Path target) throws IOException {
        if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(target);
        }
    }

    private Path requireParent(Path target) {
        Path parent = target.getParent();
        if (parent == null) {
            throw new UnsupportedOperationException(ToolFetchLogger.message("exception.archive-extract.missing-parent", target));
        }
        return parent;
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
        int perms = unixPermissions & 0_770;

        try {
            Set<PosixFilePermission> posixPermissions = Files.getPosixFilePermissions(target);

            posixPermissions.remove(PosixFilePermission.OWNER_READ);
            posixPermissions.remove(PosixFilePermission.OWNER_WRITE);
            posixPermissions.remove(PosixFilePermission.OWNER_EXECUTE);
            posixPermissions.remove(PosixFilePermission.GROUP_READ);
            posixPermissions.remove(PosixFilePermission.GROUP_WRITE);
            posixPermissions.remove(PosixFilePermission.GROUP_EXECUTE);

            if ((perms & 0_400) != 0) posixPermissions.add(PosixFilePermission.OWNER_READ);
            if ((perms & 0_200) != 0) posixPermissions.add(PosixFilePermission.OWNER_WRITE);
            if ((perms & 0_100) != 0) posixPermissions.add(PosixFilePermission.OWNER_EXECUTE);

            if ((perms & 0_040) != 0) posixPermissions.add(PosixFilePermission.GROUP_READ);
            if ((perms & 0_020) != 0) posixPermissions.add(PosixFilePermission.GROUP_WRITE);
            if ((perms & 0_010) != 0) posixPermissions.add(PosixFilePermission.GROUP_EXECUTE);

            Files.setPosixFilePermissions(target, posixPermissions);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem (e.g. Windows)
            LOGGER.log(e);
        }
    }

    private void cleanup(Path archivePath, Path destinationPath) throws IOException {
        LOGGER.log("path.remove", archivePath);
        PathHelper.deleteQuietly(archivePath);
        if (FileUtils.isEmptyDirectory(destinationPath.toFile())) {
            LOGGER.log("archive-extract.path-remove", destinationPath);
            PathHelper.deleteQuietly(destinationPath);
        }
    }

    private void tryRenameAndSetExecutable(Path sourcePath, Tool tool) {
        try {
            applyUnixPermissions(sourcePath, 0_770);
            LOGGER.log("archive-extract.set-executable", sourcePath);
            Path targetPath = sourcePath.resolveSibling(tool.id() + getFileSuffix(sourcePath));
            if (!sourcePath.equals(targetPath)) {
                Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
                LOGGER.log("path.rename", sourcePath, targetPath);
            }
        } catch (IOException e) {
            LOGGER.log(e);
        }
    }

    private String getFileSuffix(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return StringUtils.EMPTY;
        }
        String filename = fileName.toString();
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == filename.length() - 1) {
            return StringUtils.EMPTY;
        }
        return filename.substring(dot);
    }

}
