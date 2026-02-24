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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// OOC Tests for [Pack200ArchiveUncompressor]
class Pack200ArchiveUncompressorTest {

    Pack200ArchiveUncompressor uncompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uncompressor = new Pack200ArchiveUncompressor();
    }

    @ParameterizedTest
    @ValueSource(strings = {"toolfetch.gz", "toolfetch.pack.gz"})
    void testUncompress(String archiveName) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(InputStream.nullInputStream());
        InputStream cis = uncompressor.createCompressorInputStream(bis, tempDir.resolve(archiveName));
        assertThat(cis).isEqualTo(bis);
    }

}
