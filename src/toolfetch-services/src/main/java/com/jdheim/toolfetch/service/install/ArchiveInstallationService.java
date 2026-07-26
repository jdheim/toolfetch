/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.install.crypto.checksum.ChecksumService;
import com.jdheim.toolfetch.service.install.crypto.checksum.FileChecksumService;
import com.jdheim.toolfetch.service.install.download.DownloadService;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.service.install.extract.ExtractService;
import com.jdheim.toolfetch.service.install.resolve.DestinationResolver;
import com.jdheim.toolfetch.service.install.resolve.ToolDestinationResolver;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveInstallationService implements InstallationService {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveInstallationService.class);

    private final DownloadService downloadService;

    private final ChecksumService checksumService;

    private final ExtractService extractService;

    private final DestinationResolver destinationResolver;

    public ArchiveInstallationService() {
        downloadService = new WebDownloadService();
        checksumService = new FileChecksumService();
        extractService = new ArchiveExtractService();
        destinationResolver = new ToolDestinationResolver();
    }

    @Override
    public void install(Configuration configuration) {
        List<Tool> tools = configuration.tools();
        tools.forEach(tool -> {
            downloadService().download(configuration, tool).ifPresent(archivePath -> {
                if (checksumService().verify(tool, archivePath)) {
                    extractService().extract(configuration, tool, archivePath);
                } else {
                    LOG.warn("Checksum verification failed. Skipping {}", tool.id());
                }
            });
            cleanup(configuration, tool);
        });
    }

    void cleanup(Configuration configuration, Tool tool) {
        Path destinationPath = destinationResolver().resolve(configuration, tool);
        Path backupPath = destinationPath.resolveSibling(destinationPath.getFileName() + ".bak");
        if (Files.exists(destinationPath) && Files.exists(backupPath)) {
            LOG.info("Removing {}", backupPath);
            FileUtils.deleteQuietly(backupPath.toFile());
        } else if (!Files.exists(destinationPath) && Files.exists(backupPath)) {
            LOG.info("Reverting {} to {}", backupPath, destinationPath);
            try {
                Files.move(backupPath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                LOG.warn("Revert failed due to exception: \"{}: {}\"", e.getClass().getName(), e.getMessage());
            }
        }
    }

    DownloadService downloadService() {
        return downloadService;
    }

    ChecksumService checksumService() {
        return checksumService;
    }

    ExtractService extractService() {
        return extractService;
    }

    DestinationResolver destinationResolver() {
        return destinationResolver;
    }

}
