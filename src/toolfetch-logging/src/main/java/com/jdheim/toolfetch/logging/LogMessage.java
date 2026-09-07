/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging;

import java.util.Locale;

record LogMessage(LogLevel level, String message) {

    private static final char COMMA_CHAR = ',';

    private static final String PLACEHOLDER = "{}";

    static LogMessage parse(String fullMessage) {
        int delimiterIndex = requireDelimiterIndex(fullMessage);
        String message = fullMessage.substring(delimiterIndex + 1).trim();
        validateMessage(fullMessage, message);
        String rawLevel = fullMessage.substring(0, delimiterIndex).trim();
        LogLevel level = parseLevel(rawLevel, fullMessage);
        return new LogMessage(level, message);
    }

    private static int requireDelimiterIndex(String fullMessage) {
        int delimiterIndex = fullMessage.indexOf(COMMA_CHAR);
        if (delimiterIndex <= 0) {
            throw new IllegalArgumentException(
                    "Invalid log message '%s'. Expected format: <level>,<message>".formatted(fullMessage));
        }
        return delimiterIndex;
    }

    private static void validateMessage(String fullMessage, String message) {
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Log message must not be empty: '%s'".formatted(fullMessage));
        } else if (message.contains(PLACEHOLDER)) {
            throw new IllegalArgumentException("Log message requires an argument: %s".formatted(fullMessage));
        }
    }

    private static LogLevel parseLevel(String rawLevel, String fullMessage) {
        LogLevel level;
        try {
            level = LogLevel.valueOf(rawLevel.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported log level '%s' in message '%s'".formatted(rawLevel, fullMessage), e);
        }
        return level;
    }

}
