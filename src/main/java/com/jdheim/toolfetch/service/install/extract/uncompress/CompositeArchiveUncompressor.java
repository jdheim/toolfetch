/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;

public class CompositeArchiveUncompressor extends AutoDetectArchiveUncompressor {

    private final List<Uncompressor> uncompressors;

    public CompositeArchiveUncompressor() {
        uncompressors = List.of(new BrotliArchiveUncompressor(), new Pack200ArchiveUncompressor(),
                new AutoDetectArchiveUncompressor());
    }

    List<Uncompressor> getUncompressors() {
        return uncompressors;
    }

    @Override
    public ArchiveInputStream<ArchiveEntry> uncompress(Path archivePath) throws IOException {
        for (Uncompressor uncompressor : getUncompressors()) {
            if (uncompressor.isApplicable(archivePath)) {
                return uncompressor.uncompress(archivePath);
            }
        }
        throw new ArchiveException("No Archiver found for the stream signature");
    }

}
