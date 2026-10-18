/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.util;

import java.nio.file.Path;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import org.apache.commons.io.file.PathUtils;

/// Utility methods for working with filesystem paths
public final class PathHelper {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(PathHelper.class);

    private PathHelper() {
        throw new AssertionError();
    }

    public static void deleteQuietly(Path path) {
        try {
            PathUtils.delete(path);
        } catch (Exception e) {
            LOGGER.log(e, "path.remove-exception", path);
        }
    }

}
