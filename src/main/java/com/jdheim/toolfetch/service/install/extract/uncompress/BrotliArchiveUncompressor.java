/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

public class BrotliArchiveUncompressor extends AutoDetectArchiveUncompressor {

    private static final String BROTLI_SUFFIX = ".br";

    @Override
    public boolean isApplicable(Path archivePath) {
        return isBrotli(archivePath);
    }

    @Override
    protected InputStream createCompressorInputStream(BufferedInputStream bis, Path archivePath) {
        try {
            CompressorInputStream cis = COMPRESSOR_STREAM_FACTORY.createCompressorInputStream(CompressorStreamFactory.BROTLI,
                    bis);
            return new BufferedInputStream(cis);
        } catch (CompressorException _) {
            return bis;
        }
    }

    private boolean isBrotli(Path archivePath) {
        return isArchive(archivePath, BROTLI_SUFFIX);
    }

}
