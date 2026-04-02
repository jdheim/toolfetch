/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.crypto.checksum;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.Tool;

public interface ChecksumService {

    boolean verify(Tool tool, Path path);

}
