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

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

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
    public ArchiveInputStream<ArchiveEntry> uncompress(Path archivePath) throws IOException {
        InputStream in = Files.newInputStream(archivePath);
        BufferedInputStream bis = new BufferedInputStream(in);
        InputStream cis = createCompressorInputStream(bis, archivePath);
        return ARCHIVE_STREAM_FACTORY.createArchiveInputStream(detect(cis), cis);
    }

    /// Creates Compressor InputStream with `mark`/`reset` support
    ///
    /// @param bis         InputStream with `mark`/`reset` support
    /// @param archivePath May be used for further computation in overriding classes
    protected InputStream createCompressorInputStream(BufferedInputStream bis, Path archivePath) throws IOException {
        ArchiveUncompressProgress progress = new ArchiveUncompressProgress(false, bis);
        while (!progress.finished()) {
            try {
                CompressorInputStream cis = COMPRESSOR_STREAM_FACTORY.createCompressorInputStream(progress.stream());
                progress = new ArchiveUncompressProgress(false, new BufferedInputStream(cis));
            } catch (CompressorException _) {
                progress = new ArchiveUncompressProgress(true, progress.stream());
            }
        }
        return progress.stream();
    }

    private String detect(InputStream in) throws ArchiveException {
        String archiverName = ArchiveStreamFactory.detect(in);
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
