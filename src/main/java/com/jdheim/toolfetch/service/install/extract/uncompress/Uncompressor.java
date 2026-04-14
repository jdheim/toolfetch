/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.IOException;
import java.nio.file.Path;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;

public interface Uncompressor {

    boolean isApplicable(Path archivePath);

    ArchiveWithCompressorInputStream uncompress(Path archivePath) throws IOException;

}
