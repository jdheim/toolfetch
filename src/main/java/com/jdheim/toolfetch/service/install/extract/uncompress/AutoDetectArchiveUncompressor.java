/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class AutoDetectArchiveUncompressor implements Uncompressor {

    protected static final CompressorStreamFactory COMPRESSOR_STREAM_FACTORY = new CompressorStreamFactory(true);

    protected static final ArchiveStreamFactory ARCHIVE_STREAM_FACTORY = new ArchiveStreamFactory();

    /// [@Patch COMPRESS-710](https://issues.apache.org/jira/browse/COMPRESS-710)
    private static final List<String> EXCLUDED_ARCHIVERS = List.of(ArchiveStreamFactory.AR, ArchiveStreamFactory.ARJ,
            ArchiveStreamFactory.CPIO, ArchiveStreamFactory.DUMP);

    @Override
    public boolean isApplicable(Path archivePath) {
        return true;
    }

    @Override
    public ArchiveWithCompressorInputStream uncompress(Path archivePath) throws IOException {
        InputStream in = Files.newInputStream(archivePath);
        BufferedInputStream bis = new BufferedInputStream(in);
        ImmutablePair<BufferedInputStream, CompressorInputStream> cis = createCompressorInputStream(bis, archivePath);
        BufferedInputStream bufferedCis = cis.getLeft();
        ArchiveInputStream<ArchiveEntry> ais = ARCHIVE_STREAM_FACTORY.createArchiveInputStream(detect(bufferedCis), bufferedCis);
        CompressorInputStream lastCis = cis.getRight();
        return new ArchiveWithCompressorInputStream(ais, lastCis);
    }

    /// Creates Compressor InputStream with `mark`/`reset` support
    ///
    /// @param bis         InputStream with `mark`/`reset` support
    /// @param archivePath May be used for further computation in overriding classes
    protected ImmutablePair<BufferedInputStream, CompressorInputStream> createCompressorInputStream(BufferedInputStream bis,
            Path archivePath) throws IOException {
        ArchiveUncompressProgress progress = new ArchiveUncompressProgress(false, bis);
        CompressorInputStream lastCis = null;
        while (!progress.finished()) {
            try {
                CompressorInputStream cis = COMPRESSOR_STREAM_FACTORY.createCompressorInputStream(progress.stream());
                lastCis = cis;
                progress = new ArchiveUncompressProgress(false, new BufferedInputStream(cis));
            } catch (CompressorException _) {
                progress = new ArchiveUncompressProgress(true, progress.stream());
            }
        }
        return ImmutablePair.of(progress.stream(), lastCis);
    }

    private String detect(BufferedInputStream bis) throws ArchiveException {
        String archiverName = ArchiveStreamFactory.detect(bis);
        if (EXCLUDED_ARCHIVERS.contains(archiverName)) {
            throw new ArchiveException("No Archiver found for the stream signature");
        }
        return archiverName;
    }

    protected boolean isArchive(Path archivePath, String archiveSuffix) {
        return Optional.of(archivePath)
                .map(Path::getFileName)
                .map(Path::toString)
                .map(archiveName -> archiveName.toLowerCase(Locale.ROOT))
                .filter(archiveName -> archiveName.endsWith(archiveSuffix))
                .isPresent();
    }

    protected record ArchiveUncompressProgress(boolean finished, BufferedInputStream stream) {

    }

}
