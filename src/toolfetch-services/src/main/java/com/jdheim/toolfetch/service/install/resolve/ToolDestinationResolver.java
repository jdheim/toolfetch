/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;

public class ToolDestinationResolver implements DestinationResolver {

    @Override
    public Path resolve(Configuration configuration, Tool tool) {
        Path resolvedDestination = tool.resolvedDestination();
        if (resolvedDestination == null) {
            resolvedDestination = configuration.resolvedDestination();
        } else if (!resolvedDestination.isAbsolute()) {
            resolvedDestination = configuration.resolvedDestination().resolve(resolvedDestination);
        }
        return resolvedDestination.resolve(tool.id());
    }

}
