/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download;

import java.nio.file.Path;
import java.util.Optional;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;

public interface DownloadService {

    Optional<Path> download(Configuration configuration, Tool tool);

}
