/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.BufferedInputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class BrotliArchiveUncompressor extends AutoDetectArchiveUncompressor {

    private static final String BROTLI_SUFFIX = ".br";

    @Override
    public boolean isApplicable(Path archivePath) {
        return isBrotli(archivePath);
    }

    @Override
    protected ImmutablePair<BufferedInputStream, CompressorInputStream> createCompressorInputStream(BufferedInputStream bis,
            Path archivePath) {
        try {
            CompressorInputStream cis = COMPRESSOR_STREAM_FACTORY.createCompressorInputStream(CompressorStreamFactory.BROTLI,
                    bis);
            return ImmutablePair.of(new BufferedInputStream(cis), cis);
        } catch (CompressorException _) {
            return ImmutablePair.of(bis, null);
        }
    }

    private boolean isBrotli(Path archivePath) {
        return isArchive(archivePath, BROTLI_SUFFIX);
    }

}
