/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config;

import java.nio.file.Path;
import java.util.Optional;
import com.jdheim.toolfetch.model.Configuration;

public interface ConfigurationService {

    Optional<Configuration> parse(Path configPath);

}
