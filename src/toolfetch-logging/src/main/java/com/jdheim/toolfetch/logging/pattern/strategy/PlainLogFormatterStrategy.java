/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern.strategy;

import ch.qos.logback.classic.spi.ILoggingEvent;

public final class PlainLogFormatterStrategy extends AbstractLogFormatterStrategy {

    private static final String PLAIN_LOG_LEVEL_FORMAT = "%s%s%s";

    @Override
    String error() {
        return PLAIN_LOG_LEVEL_FORMAT.formatted(LOG_LEVEL_START, LOG_LEVEL_ERROR, LOG_LEVEL_END);
    }

    @Override
    String warn() {
        return PLAIN_LOG_LEVEL_FORMAT.formatted(LOG_LEVEL_START, LOG_LEVEL_WARN, LOG_LEVEL_END);
    }

    @Override
    String info() {
        return PLAIN_LOG_LEVEL_FORMAT.formatted(LOG_LEVEL_START, LOG_LEVEL_INFO, LOG_LEVEL_END);
    }

    @Override
    String step() {
        return PLAIN_LOG_LEVEL_FORMAT.formatted(LOG_LEVEL_START, LOG_LEVEL_STEP, LOG_LEVEL_END);
    }

    @Override
    String stepMessage(String message) {
        String delimiter = stepDelimiter();
        return "%s %s %s".formatted(delimiter, message, delimiter);
    }

    @Override
    String debug() {
        return PLAIN_LOG_LEVEL_FORMAT.formatted(LOG_LEVEL_START, LOG_LEVEL_DEBUG, LOG_LEVEL_END);
    }

    @Override
    String trace() {
        return PLAIN_LOG_LEVEL_FORMAT.formatted(LOG_LEVEL_START, LOG_LEVEL_TRACE, LOG_LEVEL_END);
    }

    @Override
    String other(ILoggingEvent event) {
        return "[%s]".formatted(event.getLevel());
    }

}
