/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.picocli.convert;

import java.nio.file.Path;
import picocli.CommandLine;

public class PathTrimConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String value) {
        return Path.of(value.trim());
    }

}
