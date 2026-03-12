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

package com.jdheim.toolfetch.service.install.extract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.service.install.extract.scan.ArchiveScanner;
import com.jdheim.toolfetch.service.install.extract.scan.PathScanner;
import com.jdheim.toolfetch.service.install.extract.uncompress.CompositeArchiveUncompressor;
import com.jdheim.toolfetch.service.install.extract.uncompress.Uncompressor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveExtractService implements ExtractService {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveExtractService.class);

    private static final String SLASH = "/";

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
        try (ArchiveInputStream<ArchiveEntry> ais = uncompressor.uncompress(archivePath)) {
            extractStream(ais, destinationPath, topLevelDir);
        }
    }

    private void extractStream(ArchiveInputStream<ArchiveEntry> ais, Path destinationPath, String topLevelDir) throws
            IOException {
        ArchiveEntry archiveEntry;
        while ((archiveEntry = ais.getNextEntry()) != null) {
            boolean canReadEntryData = ais.canReadEntryData(archiveEntry);
            if (!canReadEntryData || archiveEntry.isDirectory()) {
                if (!canReadEntryData) {
                    LOG.warn("Can't read archive entry at \"{}\". Skipping", archiveEntry);
                }
                continue;
            }

            String entryName = stripEntryName(archiveEntry, topLevelDir);
            Path target = resolveSecureTargetPath(destinationPath, entryName);
            extractEntry(ais, target);
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

    void extractEntry(ArchiveInputStream<ArchiveEntry> ais, Path target) throws IOException {
        Path parent = target.getParent();
        if (parent == null) {
            throw new UnsupportedOperationException(
                    "Detected Zip Slip vulnerability: Archive Entry \"%s\" does not have parent directory".formatted(target));
        }
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.copy(ais, target);
    }

    String normalize(String entryName) {
        if (StringUtils.isNotEmpty(entryName) && !entryName.endsWith(SLASH)) {
            entryName = entryName + SLASH;
        }
        return entryName;
    }

}
