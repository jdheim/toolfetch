/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.uncompress;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

public interface Uncompressor {

    boolean isApplicable(Path archivePath);

    ArchiveInputStream<ArchiveEntry> uncompress(Path archivePath) throws IOException;

}
