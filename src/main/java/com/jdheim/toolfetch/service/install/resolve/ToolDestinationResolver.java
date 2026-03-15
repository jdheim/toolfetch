/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import org.apache.commons.lang3.StringUtils;

public class ToolDestinationResolver implements DestinationResolver {

    @Override
    public Path resolve(Configuration configuration, Tool tool) {
        return Path.of("%s/%s".formatted(StringUtils.firstNonEmpty(tool.destination(), configuration.destination()), tool.id()));
    }

}
