/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.service.install.crypto.checksum.ChecksumService;
import com.jdheim.toolfetch.service.install.crypto.checksum.FileChecksumService;
import com.jdheim.toolfetch.service.install.download.DownloadService;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.service.install.extract.ExtractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveInstallationService implements InstallationService {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveInstallationService.class);

    private final DownloadService downloadService;

    private final ChecksumService checksumService;

    private final ExtractService extractService;

    public ArchiveInstallationService() {
        downloadService = new WebDownloadService();
        checksumService = new FileChecksumService();
        extractService = new ArchiveExtractService();
    }

    @Override
    public void install(Configuration configuration) {
        List<Tool> tools = configuration.tools();
        tools.forEach(tool -> downloadService.download(configuration, tool).ifPresent(archivePath -> {
            if (checksumService.verify(tool, archivePath)) {
                extractService.extract(configuration, tool, archivePath);
            } else {
                LOG.warn("Checksum verification failed. Skipping {}", tool.id());
            }
        }));
    }

}
