/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;

public interface ExtractService {

    void extract(Configuration configuration, Tool tool, Path archivePath);

}
