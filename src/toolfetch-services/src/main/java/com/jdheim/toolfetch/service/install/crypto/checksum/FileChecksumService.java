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
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.model.tool.checksums.Checksums;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

public class FileChecksumService implements ChecksumService {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(FileChecksumService.class);

    @Override
    public boolean verify(Tool tool, Path path) {
        Checksums checksums = tool.checksums();
        if (checksums == null || checksums.values() == null || checksums.values().isEmpty()) {
            LOGGER.log("checksum.missing");
            return true;
        }
        try {
            return checksums.values().entrySet().stream().allMatch(entry -> {
                Checksums.Algorithm algorithm = entry.getKey();
                String expectedChecksum = entry.getValue();
                String actualChecksum = computeMessageDigest(path, algorithm);
                boolean match = expectedChecksum.equalsIgnoreCase(actualChecksum);
                if (match) {
                    LOGGER.log("checksum.passed", algorithm, tool.id());
                } else {
                    LOGGER.log("checksum.failed", algorithm, tool.id(), expectedChecksum, actualChecksum);
                }
                return match;
            });
        } catch (Exception e) {
            LOGGER.log("checksum.exception", e.getClass().getName(), StringUtils.trimToEmpty(e.getMessage()));
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
