/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config.parse;

import java.nio.file.Path;
import java.util.Optional;

public interface ParserService {

    Optional<Object> parse(Path configPath);

}
