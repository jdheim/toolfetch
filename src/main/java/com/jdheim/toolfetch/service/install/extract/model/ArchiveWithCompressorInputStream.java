/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.model;

import java.io.IOException;
import com.jdheim.toolfetch.service.install.extract.validation.ArchiveZipBombValidator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.jspecify.annotations.Nullable;

public class ArchiveWithCompressorInputStream implements AutoCloseable, InputStreamStatistics {

    private final ArchiveInputStream<? extends ArchiveEntry> ais;

    private final @Nullable CompressorInputStream lastCis;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Streams are intentionally stored. Cannot be defensively copied")
    public ArchiveWithCompressorInputStream(ArchiveInputStream<? extends ArchiveEntry> ais,
            @Nullable CompressorInputStream lastCis) {
        this.ais = ais;
        this.lastCis = lastCis;
    }

    /**
     * Security Hotspot (java:S5042) - Expanding archive files without controlling resource consumption is security-sensitive. See {@link ArchiveZipBombValidator}
     */
    @SuppressWarnings("java:S5042")
    public ArchiveEntry getNextEntry() throws IOException {
        return ais.getNextEntry();
    }

    public boolean canReadEntryData(final ArchiveEntry archiveEntry) {
        return ais.canReadEntryData(archiveEntry);
    }

    public int read(byte[] b) throws IOException {
        return ais.read(b);
    }

    public boolean hasInputStreamStatistics() {
        return ais instanceof InputStreamStatistics || lastCis instanceof InputStreamStatistics;
    }

    @Override
    public long getCompressedCount() {
        if (ais instanceof InputStreamStatistics streamStatistics) {
            return streamStatistics.getCompressedCount();
        }
        if (lastCis instanceof InputStreamStatistics streamStatistics) {
            return streamStatistics.getCompressedCount();
        }
        return 0;
    }

    @Override
    public long getUncompressedCount() {
        if (ais instanceof InputStreamStatistics streamStatistics) {
            return streamStatistics.getUncompressedCount();
        }
        if (lastCis instanceof InputStreamStatistics streamStatistics) {
            return streamStatistics.getUncompressedCount();
        }
        return 0;
    }

    @Override
    public void close() throws IOException {
        ais.close();
    }

}
