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
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;

public class Pack200ArchiveUncompressor extends AutoDetectArchiveUncompressor {

    private static final String PACK200_SUFFIX = ".pack";

    private static final String PACK200_GZIP_SUFFIX = ".pack.gz";

    @Override
    public boolean isApplicable(Path archivePath) {
        return isPack200(archivePath) || isPack200Gz(archivePath);
    }

    @Override
    protected InputStream createCompressorInputStream(BufferedInputStream bis, Path archivePath) throws IOException {
        try {
            if (isPack200(archivePath)) {
                return getPack200InputStream(bis);
            } else if (isPack200Gz(archivePath)) {
                CompressorInputStream cis = COMPRESSOR_STREAM_FACTORY.createCompressorInputStream(bis);
                return getPack200InputStream(new BufferedInputStream(cis));
            }
        } catch (CompressorException _) {
            return bis;
        }
        return bis;
    }

    private boolean isPack200(Path archivePath) {
        return isArchive(archivePath, PACK200_SUFFIX);
    }

    private boolean isPack200Gz(Path archivePath) {
        return isArchive(archivePath, PACK200_GZIP_SUFFIX);
    }

    /// See [The Pack200 package](https://commons.apache.org/proper/commons-compress/pack200.html).
    /// The Pack200-API provided by the java class library is not streaming friendly as it wants to consume its input completely in a single operation.
    /// Because of this Pack200CompressorInputStream's constructor will immediately unpack the stream,
    /// cache the results and provide an input stream to the cache.
    /// This means we cannot use [CompressorStreamFactory#detect(InputStream)]
    private BufferedInputStream getPack200InputStream(BufferedInputStream current) throws IOException {
        return new BufferedInputStream(new Pack200CompressorInputStream(current));
    }

}
