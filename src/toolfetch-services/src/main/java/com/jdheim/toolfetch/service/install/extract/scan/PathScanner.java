/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import java.io.IOException;
import java.nio.file.Path;

public interface PathScanner {

    String scan(Path archivePath) throws IOException;

}
