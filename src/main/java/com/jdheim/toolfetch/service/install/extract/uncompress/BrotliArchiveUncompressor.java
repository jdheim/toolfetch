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
