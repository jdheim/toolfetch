/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.crypto.checksum;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jdheim.toolfetch.model.Checksums;
import com.jdheim.toolfetch.model.Tool;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChecksumService implements ChecksumService {

    private static final Logger LOG = LoggerFactory.getLogger(FileChecksumService.class);

    @Override
    public boolean verify(Tool tool, Path path) {
        Checksums checksums = tool.checksums();
        if (checksums == null || checksums.values() == null || checksums.values().isEmpty()) {
            LOG.warn("No checksums specified. Skipping checksum verification");
            return true;
        }
        try {
            return checksums.values().entrySet().stream().allMatch(entry -> {
                Checksums.Algorithm algorithm = entry.getKey();
                String expectedChecksum = entry.getValue();
                String actualChecksum = computeMessageDigest(path, algorithm);
                boolean match = expectedChecksum.equalsIgnoreCase(actualChecksum);
                if (match) {
                    LOG.info("Checksum {} verification passed for {}", algorithm, tool.id());
                } else {
                    LOG.warn("Checksum {} verification failed for {} (expected={}, actual={})", algorithm, tool.id(),
                            expectedChecksum, actualChecksum);
                }
                return match;
            });
        } catch (Exception e) {
            LOG.warn("Checksum verification failed due to exception: \"{}: {}\"", e.getClass().getName(), e.getMessage());
            return false;
        }
    }

    String computeMessageDigest(Path path, Checksums.Algorithm algorithm) {
        try (InputStream in = Files.newInputStream(path)) {
            return new DigestUtils(algorithm.toString()).digestAsHex(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
