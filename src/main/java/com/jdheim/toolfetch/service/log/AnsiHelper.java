/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.log;

import static org.fusesource.jansi.Ansi.ansi;

import com.jdheim.toolfetch.model.Tool;

public final class AnsiHelper {

    private static final String HEADER_DELIMITER = "@|bold,cyan ===|@";

    AnsiHelper() {
        throw new AssertionError();
    }

    public static String header(Tool tool) {
        String installMessage = "@|bold Installing " + tool.id() + "|@";
        return ansi().render("%s %s %s".formatted(HEADER_DELIMITER, installMessage, HEADER_DELIMITER)).toString();
    }

}
